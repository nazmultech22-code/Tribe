-- Public tours are intended to be discoverable before sign-in.
drop policy if exists "Public tours readable without account" on public.tours;
create policy "Public tours readable without account"
  on public.tours for select to anon
  using (is_public = true);
grant select on public.tours to anon;
