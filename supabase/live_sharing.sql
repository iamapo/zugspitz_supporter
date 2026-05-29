create table if not exists public.runs (
    run_code text primary key,
    estimate jsonb not null,
    created_at_epoch_millis bigint not null
);

create table if not exists public.events (
    id text primary key,
    run_code text not null references public.runs(run_code) on delete cascade,
    station_section integer not null,
    station_name text not null,
    type text not null check (type in ('CheckIn', 'CheckOut')),
    race_minutes integer not null,
    created_at_epoch_millis bigint not null
);

create table if not exists public.supporter_push_tokens (
    run_code text not null references public.runs(run_code) on delete cascade,
    platform text not null check (platform in ('ios')),
    device_token text not null,
    is_enabled boolean not null default true,
    created_at_epoch_millis bigint not null,
    updated_at_epoch_millis bigint not null,
    primary key (run_code, platform, device_token)
);

alter table public.runs enable row level security;
alter table public.events enable row level security;
alter table public.supporter_push_tokens enable row level security;

drop policy if exists "authenticated can read runs" on public.runs;
create policy "authenticated can read runs"
on public.runs
for select
to authenticated
using (true);

drop policy if exists "authenticated can write runs" on public.runs;
create policy "authenticated can write runs"
on public.runs
for all
to authenticated
using (true)
with check (true);

drop policy if exists "authenticated can read events" on public.events;
create policy "authenticated can read events"
on public.events
for select
to authenticated
using (true);

drop policy if exists "authenticated can write events" on public.events;
create policy "authenticated can write events"
on public.events
for all
to authenticated
using (true)
with check (true);

grant select, insert, update on public.runs to authenticated;
grant select, insert, update on public.events to authenticated;

create or replace function public.upsert_supporter_push_token(
    p_run_code text,
    p_platform text,
    p_device_token text,
    p_is_enabled boolean,
    p_now_epoch_millis bigint
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if p_platform <> 'ios' then
        raise exception 'Unsupported push platform: %', p_platform;
    end if;

    if p_run_code is null or btrim(p_run_code) = '' or p_device_token is null or btrim(p_device_token) = '' then
        return;
    end if;

    if not exists (
        select 1
        from public.runs
        where run_code = p_run_code
    ) then
        return;
    end if;

    insert into public.supporter_push_tokens (
        run_code,
        platform,
        device_token,
        is_enabled,
        created_at_epoch_millis,
        updated_at_epoch_millis
    )
    values (
        p_run_code,
        p_platform,
        p_device_token,
        p_is_enabled,
        p_now_epoch_millis,
        p_now_epoch_millis
    )
    on conflict (run_code, platform, device_token)
    do update set
        is_enabled = excluded.is_enabled,
        updated_at_epoch_millis = excluded.updated_at_epoch_millis;
end;
$$;

grant execute on function public.upsert_supporter_push_token(text, text, text, boolean, bigint) to authenticated;

create or replace function public.create_live_run(
    p_estimate jsonb,
    p_created_at_epoch_millis bigint
)
returns public.runs
language plpgsql
security invoker
as $$
declare
    v_code text;
    v_row public.runs;
    v_chars constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    v_length constant integer := 8;
    v_attempt integer := 0;
    v_index integer;
begin
    loop
        v_attempt := v_attempt + 1;
        v_code := '';

        for v_index in 1..v_length loop
            v_code := v_code || substr(v_chars, 1 + floor(random() * length(v_chars))::int, 1);
        end loop;

        begin
            insert into public.runs (
                run_code,
                estimate,
                created_at_epoch_millis
            )
            values (
                v_code,
                p_estimate,
                p_created_at_epoch_millis
            )
            returning * into v_row;

            return v_row;
        exception
            when unique_violation then
                if v_attempt >= 20 then
                    raise exception 'Could not generate unique run code after % attempts', v_attempt;
                end if;
        end;
    end loop;
end;
$$;

grant execute on function public.create_live_run(jsonb, bigint) to authenticated;
