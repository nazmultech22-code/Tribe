create or replace function public.review_tour_join_request(
  p_request_id uuid,
  p_decision text
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_tour_id uuid;
  v_requester_id uuid;
  v_status public.join_request_status;
begin
  if auth.uid() is null then raise exception 'authentication_required'; end if;
  if p_decision not in ('approved', 'rejected') then raise exception 'invalid_join_request_decision'; end if;

  select tour_id, requester_id, status into v_tour_id, v_requester_id, v_status
  from public.tour_join_requests where id = p_request_id for update;

  if not found then raise exception 'join_request_not_found'; end if;
  if not public.can_manage_tour_members(v_tour_id) then raise exception 'not_authorized'; end if;
  if v_status <> 'pending'::public.join_request_status then return false; end if;

  if p_decision = 'approved' then
    insert into public.tour_members(tour_id, user_id, role)
    values (v_tour_id, v_requester_id, 'member'::public.member_role)
    on conflict (tour_id, user_id) do nothing;
  end if;

  update public.tour_join_requests
  set status = p_decision::public.join_request_status, reviewed_at = now()
  where id = p_request_id;
  return true;
end;
$function$;

revoke all on function public.review_tour_join_request(uuid, text) from public;
grant execute on function public.review_tour_join_request(uuid, text) to authenticated;
