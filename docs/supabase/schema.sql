-- Jalilgym cloud sync schema. Run once in the Supabase SQL editor
-- (docs/supabase/SETUP.md). Safe to re-run.
--
-- Every synced row is stored as JSON, keyed by the app's table and primary key.
-- The app is the source of truth for the schema; the server only keeps the
-- newest version of each row and hands out changes in order.

create sequence if not exists public.sync_seq;

create table if not exists public.sync_rows (
    user_id    uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    table_name text    not null,
    row_key    text    not null,
    data       jsonb,
    deleted    boolean not null default false,
    -- When the row last changed on a device, epoch millis. The newest wins.
    updated_at bigint  not null,
    -- Order of arrival on the server; devices pull everything after the last seq they saw.
    seq        bigint  not null default nextval('public.sync_seq'),
    primary key (user_id, table_name, row_key)
);

create index if not exists sync_rows_user_seq on public.sync_rows (user_id, seq);

alter table public.sync_rows enable row level security;

drop policy if exists "Own rows only" on public.sync_rows;
create policy "Own rows only" on public.sync_rows
    for all to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- Upserts a batch of changes; an older change never overwrites a newer one.
create or replace function public.push_rows(rows jsonb)
returns void
language sql
security invoker
set search_path = ''
as $$
    insert into public.sync_rows (user_id, table_name, row_key, data, deleted, updated_at, seq)
    select auth.uid(),
           r ->> 'table_name',
           r ->> 'row_key',
           r -> 'data',
           coalesce((r ->> 'deleted')::boolean, false),
           (r ->> 'updated_at')::bigint,
           nextval('public.sync_seq')
    from jsonb_array_elements(rows) as r
    on conflict (user_id, table_name, row_key) do update
        set data = excluded.data,
            deleted = excluded.deleted,
            updated_at = excluded.updated_at,
            seq = excluded.seq
        where public.sync_rows.updated_at <= excluded.updated_at;
$$;

revoke all on function public.push_rows(jsonb) from public, anon;
grant execute on function public.push_rows(jsonb) to authenticated;

-- Deletes the signed-in user's account and, by cascade, all their synced data.
-- Required by Google Play for apps that let people create an account.
create or replace function public.delete_my_account()
returns void
language sql
security definer
set search_path = ''
as $$
    delete from auth.users where id = auth.uid();
$$;

revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;
