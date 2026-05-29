import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

type CheckEventRecord = {
  id: string;
  run_code: string;
  station_section: number;
  station_name: string;
  type: "CheckIn" | "CheckOut";
  race_minutes: number;
  created_at_epoch_millis: number;
};

type PushTokenRow = {
  run_code: string;
  platform: "ios";
  device_token: string;
};

type WebhookBody = {
  record?: CheckEventRecord;
};

let cachedProviderToken: { value: string; expiresAtEpochSeconds: number } | null = null;

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  const webhookSecret = Deno.env.get("CHECK_IN_PUSH_WEBHOOK_SECRET");
  if (webhookSecret) {
    const suppliedSecret =
      request.headers.get("x-webhook-secret") ??
      (request.headers.get("authorization") ?? "").replace(/^Bearer\s+/i, "");
    if (suppliedSecret !== webhookSecret) {
      return json({ error: "Unauthorized" }, 401);
    }
  }

  const body = await request.json().catch(() => null) as WebhookBody | null;
  const event = body?.record;
  const message = event ? notificationMessage(event) : null;
  if (!message) {
    return json({ sent: 0, skipped: true });
  }

  const supabaseUrl = requiredEnv("SUPABASE_URL");
  const serviceRoleKey = requiredEnv("SERVICE_ROLE_KEY");
  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false },
  });

  const { data: tokens, error } = await supabase
    .from("supporter_push_tokens")
    .select("run_code, platform, device_token")
    .eq("run_code", event.run_code)
    .eq("platform", "ios")
    .eq("is_enabled", true);

  if (error) {
    console.error("Could not load supporter push tokens", error);
    return json({ error: "Could not load push tokens" }, 500);
  }

  if (!tokens?.length) {
    return json({ sent: 0, failed: 0 });
  }

  const payload = {
    aps: {
      alert: {
        title: message,
      },
      sound: "default",
    },
    eventId: event.id,
    runCode: event.run_code,
    stationSection: event.station_section,
    eventType: event.type,
  };

  const results = await Promise.all(
    (tokens as PushTokenRow[]).map((token) => sendApnsNotification(token, payload)),
  );
  const staleTokens = results
    .filter((result) => result.disableToken)
    .map((result) => result.deviceToken);

  await Promise.all(
    staleTokens.map((deviceToken) =>
      supabase
        .from("supporter_push_tokens")
        .update({
          is_enabled: false,
          updated_at_epoch_millis: Date.now(),
        })
        .eq("run_code", event.run_code)
        .eq("platform", "ios")
        .eq("device_token", deviceToken)
    ),
  );

  return json({
    sent: results.filter((result) => result.ok).length,
    failed: results.filter((result) => !result.ok).length,
    disabled: staleTokens.length,
  });
});

function notificationMessage(event: CheckEventRecord): string | null {
  if (isStartEvent(event)) {
    return "Läufer gestartet";
  }

  if (isFinishEvent(event)) {
    return event.type === "CheckIn" ? "Läufer ist im Ziel" : null;
  }

  const stationLabel = `VP${event.station_section} ${event.station_name}`;
  return event.type === "CheckIn"
    ? `Läufer am ${stationLabel} eingecheckt`
    : `Läufer am ${stationLabel} ausgecheckt`;
}

function isStartEvent(event: CheckEventRecord): boolean {
  return event.type === "CheckIn" && (
    event.station_section === 0 ||
    event.station_name.trim().toLowerCase() === "start"
  );
}

function isFinishEvent(event: CheckEventRecord): boolean {
  return event.station_name.trim().toLowerCase().startsWith("ziel");
}

async function sendApnsNotification(
  token: PushTokenRow,
  payload: unknown,
): Promise<{ ok: boolean; deviceToken: string; disableToken: boolean }> {
  const bundleId = requiredEnv("APNS_BUNDLE_ID");
  const providerToken = await getProviderToken();
  const host = (Deno.env.get("APNS_ENVIRONMENT") ?? "development").toLowerCase() === "production"
    ? "https://api.push.apple.com"
    : "https://api.sandbox.push.apple.com";

  const response = await fetch(`${host}/3/device/${token.device_token}`, {
    method: "POST",
    headers: {
      authorization: `bearer ${providerToken}`,
      "apns-topic": bundleId,
      "apns-push-type": "alert",
      "apns-priority": "10",
      "content-type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (response.ok) {
    return { ok: true, deviceToken: token.device_token, disableToken: false };
  }

  const responseBody = await response.text();
  console.error("APNs push failed", response.status, responseBody);
  const shouldDisable =
    response.status === 410 ||
    responseBody.includes("BadDeviceToken") ||
    responseBody.includes("Unregistered");

  return {
    ok: false,
    deviceToken: token.device_token,
    disableToken: shouldDisable,
  };
}

async function getProviderToken(): Promise<string> {
  const nowEpochSeconds = Math.floor(Date.now() / 1000);
  if (cachedProviderToken && cachedProviderToken.expiresAtEpochSeconds - 60 > nowEpochSeconds) {
    return cachedProviderToken.value;
  }

  const keyId = requiredEnv("APNS_KEY_ID");
  const teamId = requiredEnv("APNS_TEAM_ID");
  const privateKeyPem = requiredEnv("APNS_PRIVATE_KEY").replace(/\\n/g, "\n");
  const header = base64Url(JSON.stringify({ alg: "ES256", kid: keyId }));
  const claims = base64Url(JSON.stringify({ iss: teamId, iat: nowEpochSeconds }));
  const signingInput = `${header}.${claims}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign(
      { name: "ECDSA", hash: "SHA-256" },
      key,
      new TextEncoder().encode(signingInput),
    ),
  );
  const jwt = `${signingInput}.${base64Url(ecdsaSignatureToJose(signature))}`;
  cachedProviderToken = {
    value: jwt,
    expiresAtEpochSeconds: nowEpochSeconds + 50 * 60,
  };
  return jwt;
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes.buffer;
}

function ecdsaSignatureToJose(signature: Uint8Array): Uint8Array {
  if (signature.length === 64) return signature;
  if (signature[0] !== 0x30) return signature;

  let offset = 2;
  if (signature[1] & 0x80) {
    offset = 2 + (signature[1] & 0x7f);
  }
  const rLength = signature[offset + 1];
  const r = signature.slice(offset + 2, offset + 2 + rLength);
  offset = offset + 2 + rLength;
  const sLength = signature[offset + 1];
  const s = signature.slice(offset + 2, offset + 2 + sLength);
  return concatFixedInteger(r, s);
}

function concatFixedInteger(r: Uint8Array, s: Uint8Array): Uint8Array {
  const result = new Uint8Array(64);
  result.set(trimAndPadInteger(r), 0);
  result.set(trimAndPadInteger(s), 32);
  return result;
}

function trimAndPadInteger(value: Uint8Array): Uint8Array {
  const trimmed = value[0] === 0 ? value.slice(1) : value;
  const result = new Uint8Array(32);
  result.set(trimmed.slice(-32), 32 - Math.min(trimmed.length, 32));
  return result;
}

function base64Url(value: string | Uint8Array): string {
  const binary = typeof value === "string"
    ? value
    : Array.from(value, (byte) => String.fromCharCode(byte)).join("");
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function requiredEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing required environment variable ${name}`);
  return value;
}

function json(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "content-type": "application/json" },
  });
}
