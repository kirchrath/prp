-- At most one open automatic item per article: concurrent syncs (retried events run in parallel) fail on this
-- index and are retried instead of creating a second item. Older duplicates are soft deleted first.
update shopping_item s
set deleted = true
where s.automatic and not s.deleted
  and exists (select 1 from shopping_item o
              where o.automatic and not o.deleted and o.article_id = s.article_id
                and (o.created_date, o.id) > (s.created_date, s.id));
drop index idx_item_auto_article;
create unique index idx_item_auto_article on shopping_item (article_id) where automatic and not deleted;
