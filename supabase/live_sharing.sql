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

alter table public.runs enable row level security;
alter table public.events enable row level security;

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
