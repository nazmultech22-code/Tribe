-- Additive TRIBE destination catalog and guide support.
-- Existing tables and user data are not altered or removed.

create table if not exists public.destinations (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(trim(name)) between 2 and 120),
  country text,
  region text,
  description text,
  best_season text,
  transport_info text,
  accommodation_info text,
  food_info text,
  guide_info text,
  safety_tips text,
  image_url text,
  latitude double precision,
  longitude double precision,
  is_featured boolean not null default false,
  is_active boolean not null default true,
  created_by uuid not null default auth.uid() references public.profiles(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create unique index if not exists destinations_name_country_uidx
  on public.destinations (lower(name), lower(coalesce(country, '')));
create index if not exists destinations_active_name_idx on public.destinations (is_active, name);
alter table public.destinations enable row level security;
drop policy if exists "Active destinations are publicly readable" on public.destinations;
create policy "Active destinations are publicly readable" on public.destinations for select to anon, authenticated
  using (is_active = true or created_by = (select auth.uid()));
drop policy if exists "Users create destinations they own" on public.destinations;
create policy "Users create destinations they own" on public.destinations for insert to authenticated
  with check (created_by = (select auth.uid()));
drop policy if exists "Owners update destinations" on public.destinations;
create policy "Owners update destinations" on public.destinations for update to authenticated
  using (created_by = (select auth.uid())) with check (created_by = (select auth.uid()));
drop policy if exists "Owners delete destinations" on public.destinations;
create policy "Owners delete destinations" on public.destinations for delete to authenticated
  using (created_by = (select auth.uid()));
grant select on public.destinations to anon, authenticated;
grant insert, update, delete on public.destinations to authenticated;

create table if not exists public.destination_aliases (
  id uuid primary key default gen_random_uuid(),
  destination_id uuid not null references public.destinations(id) on delete cascade,
  alias text not null check (char_length(trim(alias)) between 1 and 120),
  language text check (language in ('bn', 'en') or language is null),
  created_at timestamptz not null default now()
);
create unique index if not exists destination_aliases_alias_language_uidx
  on public.destination_aliases (lower(alias), coalesce(language, ''));
create index if not exists destination_aliases_destination_idx on public.destination_aliases (destination_id);
alter table public.destination_aliases enable row level security;
drop policy if exists "Aliases readable with active destination" on public.destination_aliases;
create policy "Aliases readable with active destination" on public.destination_aliases for select to anon, authenticated
  using (exists (select 1 from public.destinations d where d.id = destination_id and (d.is_active = true or d.created_by = (select auth.uid()))));
drop policy if exists "Destination owners manage aliases" on public.destination_aliases;
create policy "Destination owners manage aliases" on public.destination_aliases for all to authenticated
  using (exists (select 1 from public.destinations d where d.id = destination_id and d.created_by = (select auth.uid())))
  with check (exists (select 1 from public.destinations d where d.id = destination_id and d.created_by = (select auth.uid())));
grant select on public.destination_aliases to anon, authenticated;
grant insert, update, delete on public.destination_aliases to authenticated;

create table if not exists public.tour_guides (
  id uuid primary key default gen_random_uuid(),
  tour_id uuid references public.tours(id) on delete cascade,
  destination_id uuid references public.destinations(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 120),
  phone text,
  email text,
  created_by uuid not null default auth.uid() references public.profiles(id) on delete restrict,
  created_at timestamptz not null default now(),
  constraint tour_guides_parent_required check (tour_id is not null or destination_id is not null)
);
create index if not exists tour_guides_tour_idx on public.tour_guides(tour_id);
create index if not exists tour_guides_destination_idx on public.tour_guides(destination_id);
alter table public.tour_guides enable row level security;
drop policy if exists "Guides visible to destination visitors or tour members" on public.tour_guides;
create policy "Guides visible to destination visitors or tour members" on public.tour_guides for select to authenticated
  using ((destination_id is not null and exists (select 1 from public.destinations d where d.id = destination_id and d.is_active = true))
    or (tour_id is not null and public.is_tour_collaborator(tour_id)));
drop policy if exists "Authorized users create guides" on public.tour_guides;
create policy "Authorized users create guides" on public.tour_guides for insert to authenticated
  with check (created_by = (select auth.uid())
    and (tour_id is null or public.can_manage_tour_members(tour_id))
    and (destination_id is null or exists (select 1 from public.destinations d where d.id = destination_id and d.created_by = (select auth.uid()))));
drop policy if exists "Guide creators or tour managers update guides" on public.tour_guides;
create policy "Guide creators or tour managers update guides" on public.tour_guides for update to authenticated
  using (created_by = (select auth.uid()) or (tour_id is not null and public.can_manage_tour_members(tour_id)))
  with check (created_by = (select auth.uid()) or (tour_id is not null and public.can_manage_tour_members(tour_id)));
drop policy if exists "Guide creators or tour managers delete guides" on public.tour_guides;
create policy "Guide creators or tour managers delete guides" on public.tour_guides for delete to authenticated
  using (created_by = (select auth.uid()) or (tour_id is not null and public.can_manage_tour_members(tour_id)));
grant select, insert, update, delete on public.tour_guides to authenticated;
drop trigger if exists destinations_set_updated_at on public.destinations;
create trigger destinations_set_updated_at before update on public.destinations
  for each row execute function public.set_updated_at();
