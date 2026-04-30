# DavaRoutes

## Running backend
- be in the root of the project
- docker-compose up --build

## Inserting sample data
- be in the root of the project
- docker compose exec backend python -m app.db.seed
- this should be the output:
``` 
Created user: ev.family@davaroutes.com
Created user: business.driver@davaroutes.com
Created user: budget.ev@davaroutes.com
Created user: service.due@davaroutes.com
Created driver profile: Balanced Daily Profile
Created driver profile: Family EV Profile
Created driver profile: Business Fast Profile
Created driver profile: Budget EV Profile
Created driver profile: Service Aware Profile
Created vehicle: Dacia Logan
Created vehicle: Tesla Model 3
Created vehicle: BMW 320d
Created vehicle: Hyundai Kona Electric
Created vehicle: Toyota Corolla Hybrid
Created vehicle state for vehicle: 76ced722-eeaf-4959-8ed3-50e73f44c81e
Created vehicle state for vehicle: d1a77c6b-4711-4b1b-a0f2-0d7a340f984a
Created vehicle state for vehicle: 565becbc-4e81-4d29-a23c-cd1d28b0f3a3
Created vehicle state for vehicle: c5232b73-192c-42ff-aded-ac8aab74defc
Created vehicle state for vehicle: cab1d654-1962-4778-bc5e-2510fc32bc38
Created location: Ionity Ploiesti FastCharge
Created location: Kaufland Charge Ploiesti
Created location: EcoCharge Campina
Created location: MOL Plugee Sinaia
Created location: MountainCharge Predeal
Created location: CityCharge Brasov
Created location: Hotel EV Charger Brasov
Created location: SlowCharge Busteni
Created location: OMV Ploiesti Nord
Created location: MOL Campina
Created location: Petrom Sinaia
Created location: Rompetrol Busteni
Created location: Lukoil Brasov
Created location: OMV Predeal
Created location: MOL Brasov Centru
Created location: McDonald's Ploiesti DN1
Created location: Starbucks Ploiesti
Created location: Family Grill Campina
Created location: Mountain View Cafe Sinaia
Created location: Quick Lunch Brasov
Created location: Burger House Busteni
Created location: Predeal Coffee Stop
Created location: Brasov Family Bistro
Created location: Hotel Brasov Central
Created location: Hotel Alpin Sinaia
Created location: Hotel Comfort Predeal
Created location: Dacia Authorized Service Ploiesti
Created location: Tesla Service Brasov
Created location: Bosch Car Service Campina
Created location: Toyota Authorized Service Brasov
Created location: Generic Auto Service Sinaia
Created location: Comarnic Rest Area
Created location: Peles Castle Parking
Created location: Predeal Rest Area
Created location: Brasov Old Town Parking
Created charging station for location: ff694518-7db3-4f3b-b963-624084972f2c
Created charging station for location: 3cf13b61-afe2-46f3-9dfa-ff28c0b4a199
Created charging station for location: c9170ebf-b149-4c03-ad59-2f629984044e
Created charging station for location: ec8f5dd4-5620-4102-8208-446d08b1bb5b
Created charging station for location: 83a8cd1d-e368-4cb5-b9a7-83340fdeea06
Created charging station for location: 8444a417-6938-480b-8a27-8ea48bdcc39c
Created charging station for location: 4051f6bf-6ee6-4f72-9527-b7742bd15fa6
Created charging station for location: bfd21e90-95ea-46b5-9c70-11a232e7fb44
Created fuel station for location: 9f6cefad-9b13-40d9-b099-32b13fc49222
Created fuel station for location: 19961ba1-a004-4fac-9dfb-71ae052dae16
Created fuel station for location: 540ff0d8-1359-4694-9438-872f4baa8516
Created fuel station for location: 97377677-918a-46ef-b5fc-9b943a0a5319
Created fuel station for location: 7a93c9f8-5d84-4ec8-ab70-97e330e51658
Created fuel station for location: 6af1c4a8-2c28-4281-9294-d616363bf104
Created fuel station for location: 52be0a83-fa87-47b4-800a-b19e18596861
Created service center for location: 20999dfc-a382-4ccd-b53f-29d4dc7b0bdc
Created service center for location: 8fe9ad0b-892c-4b02-8f45-ce68d03a79c9
Created service center for location: 435fbc94-bc9b-4d27-b864-a23a0049d5d8
Created service center for location: 1fc68d7e-25ee-4439-8ece-f2ae99d0f2a2
Created service center for location: c72c925a-5ae5-46f8-a877-95cdb6ed0063
Created partner: Ionity
Created partner: OMV
Created partner: MOL
Created partner: McDonald's
Created partner: Starbucks
Created partner: Kaufland
Created partner: Dacia Service Network
Created partner: Toyota Service Network
Created partner location: fcc71f9d-e5bb-48b7-9297-0141fa8ef17a -> ff694518-7db3-4f3b-b963-624084972f2c
Created partner location: e94cbe25-2f64-4db1-884b-bd1df503e6db -> 9f6cefad-9b13-40d9-b099-32b13fc49222
Created partner location: e94cbe25-2f64-4db1-884b-bd1df503e6db -> 6af1c4a8-2c28-4281-9294-d616363bf104
Created partner location: 6839caa1-00b6-4fc0-9a84-23e6264a91c2 -> 19961ba1-a004-4fac-9dfb-71ae052dae16
Created partner location: 6839caa1-00b6-4fc0-9a84-23e6264a91c2 -> 52be0a83-fa87-47b4-800a-b19e18596861
Created partner location: ebfd4726-114f-40ae-a4c3-e760762e287c -> 3bd2eb16-ca16-412c-8473-e0239fdc6578
Created partner location: 52fb9e80-10f1-44ee-b44a-422b48729fee -> b0b87a41-a0bf-413b-9d17-bc96f5d16f16
Created partner location: 7d9ff309-35e6-4812-9925-1ba614a88b28 -> 3cf13b61-afe2-46f3-9dfa-ff28c0b4a199
Created partner location: ae99ea91-71a7-4f8e-aa81-8d214c303d04 -> 20999dfc-a382-4ccd-b53f-29d4dc7b0bdc
Created partner location: db2c481f-c947-4bfb-a15d-40d45f025ab6 -> 1fc68d7e-25ee-4439-8ece-f2ae99d0f2a2
Created partner location: 7d9ff309-35e6-4812-9925-1ba614a88b28 -> 4051f6bf-6ee6-4f72-9527-b7742bd15fa6
Created partner location: 6839caa1-00b6-4fc0-9a84-23e6264a91c2 -> ec8f5dd4-5620-4102-8208-446d08b1bb5b
Created partner offer: 10% off Ionity fast charging
Created partner offer: Free parking while charging
Created partner offer: 50 loyalty points for OMV fuel stop
Created partner offer: Coffee bundle at OMV Predeal
Created partner offer: 70 loyalty points at MOL Campina
Created partner offer: MOL Plugee EV charging perk
Created partner offer: Family meal discount
Created partner offer: Business coffee stop
Created partner offer: Dacia inspection discount
Created partner offer: Toyota hybrid check bonus
Created partner offer: Destination charging hotel perk
Created partner offer: Brasov city fuel cashback
Created loyalty account for user: 3d71c48e-8a27-42d8-849c-bfac00690581
Created loyalty account for user: 2fdcbc73-3f21-421a-b9fe-6e9ab06093c9
Created loyalty account for user: 2c833e21-30e6-4860-aa0d-5f4af7990c8e
Created loyalty account for user: c238438b-af6c-460d-a225-587a00cef47c
Created loyalty account for user: fff985cb-dad6-4a1a-adbc-48f5750397fa
Seed data inserted successfully.
```
- if you want to check the inserted data, you can use the following command to access the database container:
-  docker exec -it davaroutes_postgres psql -U davaroutes_user -d davaroutes_db
- query: 
```
SELECT 'users' AS table_name, COUNT(*) FROM users
UNION ALL
SELECT 'driver_profiles', COUNT(*) FROM driver_profiles
UNION ALL
SELECT 'vehicles', COUNT(*) FROM vehicles
UNION ALL
SELECT 'vehicle_state_snapshots', COUNT(*) FROM vehicle_state_snapshots
UNION ALL
SELECT 'locations', COUNT(*) FROM locations
UNION ALL
SELECT 'charging_stations', COUNT(*) FROM charging_stations
UNION ALL
SELECT 'fuel_stations', COUNT(*) FROM fuel_stations
UNION ALL
SELECT 'service_centers', COUNT(*) FROM service_centers
UNION ALL
SELECT 'partners', COUNT(*) FROM partners
UNION ALL
SELECT 'partner_locations', COUNT(*) FROM partner_locations
UNION ALL
SELECT 'partner_offers', COUNT(*) FROM partner_offers
UNION ALL
SELECT 'loyalty_accounts', COUNT(*) FROM loyalty_accounts;
```
