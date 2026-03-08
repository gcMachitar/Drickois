create table if not exists item_supplier (
    item_id bigint not null references item(item_id) on delete cascade on update cascade,
    supplier_id bigint not null references supplier(supplier_id) on delete cascade on update cascade,
    is_primary boolean not null default true,
    created_at timestamptz not null default now(),
    primary key (item_id, supplier_id)
);

create unique index if not exists item_supplier_primary_idx
    on item_supplier (item_id)
    where is_primary = true;
