Bambu Vibe Logistics Management System
A Java Swing application for inventory and customer management

ERD-first setup
- Run [erd_schema.sql](./erd_schema.sql) in Supabase SQL editor to create the ERD tables.
- The app now maps inventory CRUD to ERD table `item`:
  - `item_name` <- Item Name
  - `unit_type` <- Category field in UI
  - `quantity_on_hand` <- Quantity
  - `unit_cost` <- Price
- Order action now follows ERD flow:
  - ensures `user`, `category`, `product`, and `ingredients` links
  - inserts `sales` and `sales_details`
  - updates `item.quantity_on_hand`
- Registration profile sync now writes to ERD table `user`.
- `action_logs` was removed from the client flow because it is not part of the ERD.

How to Run
Debug/run:
DrickSysApp
