-- Remove pre-configured demo teaching time slots from non-demo centers
DELETE t FROM `teaching_time_slots` t
JOIN `centers` c ON t.`center_id` = c.`id`
WHERE c.`subdomain` NOT IN ('owlexa-q1', 'owlexa-cg')
  AND NOT EXISTS (
    SELECT 1 FROM `schedule_recurring_rules` r WHERE r.`time_slot_id` = t.`id`
  );
