curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test9@example.com",
    "password": "Test123!"
  }' | jq


# As room admin: add user_b to room_123 as regular USER
curl -X POST http://127.0.0.1:8005/api/rooms/0beaf05f-3f45-466f-8913-9f218b0d7884/members \
  -H "Content-Type: application/json" \
  --cookie "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI1MDI2LCJpYXQiOjE3Nzg1MjQ3MjYsImp0aSI6IjA2YzcyMzgyZmU2YzQ5NDliOWYzYWI1MTU4MTdhM2RiIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.rqdHagJIlyf4v_ccLy5SIon_fRylQCHrgHWEs6_pCpr9nlodvuNwNTLZhzVcHCNlZ-SPU2SvZ8dsP44bwVEtzPJiPVfcQQSjWs3t13wnJq0ztXl2eQMRED7ueYaoMMjgxuuexCC_RHzsUDx6s355TzPGHV41O9plL9ktPFNpaAzQeFBIxMAlqjnZKNKpgM25LDxOGADllH4VI1vCmBc7DY1wv1sFqfd_ecfwLo4vKKWTwkDoxh6kEZSBXoBT2Ch0S9Mn5llFckzmJCbOsFJbWx8GWc1AMZqrrp0fYOtSbmOhpx0A8RUGzlGlJeZPrMLDqXbLe6a4tm1UPooxeXuvUQ" \
  -d '{
    "user_id": "98787ef6-f118-400c-ad64-66e5634e664c",
    "status": "USER"
  }' | jq

# Create member with ADMIN status (e.g., room creator)
curl -X POST http://127.0.0.1:8005/api/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0MTc1LCJpYXQiOjE3Nzg1MjM4NzUsImp0aSI6ImQxODUzNmJhMDA2MTQ1NDRhMTk0ZWFiNmM0NDM4ZDBlIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.I1OmV14UYbe-KFdS-Vcyk-_2IMduQA7obMrz7O43Ap1oUbGV_Dl5l-VAHdlfdg1t--lX5tOaNqDllsindCVbmO9j8oSImjwxN5EcjIlevb0EivjYhw7FnuoqlOz2qQEAh0hRraI2unGbw0LO04JjzI0vf-bgLL4vvoeqyraZLV-SQx7bAr7Hg2wImfcyhPwDiAL0vpJhNBlWf8TXO1yIJqH9-5Ks5oiL6Jj1zKocRt2dlViToxLJzfkUAD9iEu8W2SMViG_SgivOsR5dwN4CQ_hCJFrEF1-jW2L3yvaVnvSRwMdVfmqqq4-0p_wim7IFMreg8BOaBbrKdtzbxxubLA" \
  -d '{
    "user_id": "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
    "status": "ADMIN"
  }' | jq


# 1. Leave room (pretty JSON with jq)
curl -s -X DELETE \
  http://127.0.0.1:8005/api/members/a33a1703-2fec-4502-8189-60aadf1961fc/leave \
  --cookie "access_token=YOUR_ACCESS_TOKEN" | jq


# 2. Get room members (pretty JSON with jq)
curl -s -X GET \
  http://127.0.0.1:8005/api/rooms/0beaf05f-3f45-466f-8913-9f218b0d7884/members \
  --cookie "access_token=YOUR_ACCESS_TOKEN" | jq

# Admin removes another member from the room
# 🔐 Authorization check should be added in controller before this call
curl -X DELETE http://127.0.0.1:8005/api/members/2d45f6c3-fbe5-4f69-a1af-30d4ce3124e2 \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0NzU5LCJpYXQiOjE3Nzg1MjQ0NTksImp0aSI6Ijk4MTI4MmUwZjI5ODQwNWJiMmNhODE2ZThiMjIzNTY3IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RIdMuv3kfzbNvllx9NC_AUCnntQ6k10DbjY7IFVmwzVx2Cf5OTOV0DA0-eBPBKZ-rpB-0--VirohiV9knqymdNWEKDhur6pNzdDoNRd-yTmni2Dq0dYxxliXcxPFS_6fT9b-jIxo2U9V9F_U9RXTSEP-9mCRglLXErnN-WMXIVMPsnWB44qOGWSSogoxavCWKFINu0OB4agMCmS6dOnr0xG41kx7yJ5e4imk5Cjd5Nttz7cKretZT1Ts6samCqQzol19S2wj9BcQ0RIk8pJ2QH-cURjcOVv0Fmd95y1dxjYR9FALhmaKJub2FdFRYWaJnOrCmPOdLfk9f4N7sppEJQ" | jq

# Promote a member to ADMIN
curl -X PATCH http://127.0.0.1:8005/api/members/2d45f6c3-fbe5-4f69-a1af-30d4ce3124e2/promote \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0NzU5LCJpYXQiOjE3Nzg1MjQ0NTksImp0aSI6Ijk4MTI4MmUwZjI5ODQwNWJiMmNhODE2ZThiMjIzNTY3IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RIdMuv3kfzbNvllx9NC_AUCnntQ6k10DbjY7IFVmwzVx2Cf5OTOV0DA0-eBPBKZ-rpB-0--VirohiV9knqymdNWEKDhur6pNzdDoNRd-yTmni2Dq0dYxxliXcxPFS_6fT9b-jIxo2U9V9F_U9RXTSEP-9mCRglLXErnN-WMXIVMPsnWB44qOGWSSogoxavCWKFINu0OB4agMCmS6dOnr0xG41kx7yJ5e4imk5Cjd5Nttz7cKretZT1Ts6samCqQzol19S2wj9BcQ0RIk8pJ2QH-cURjcOVv0Fmd95y1dxjYR9FALhmaKJub2FdFRYWaJnOrCmPOdLfk9f4N7sppEJQ" | jq

# Demote a member to USER
curl -X PATCH http://127.0.0.1:8005/api/members/2d45f6c3-fbe5-4f69-a1af-30d4ce3124e2/demote \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0NzU5LCJpYXQiOjE3Nzg1MjQ0NTksImp0aSI6Ijk4MTI4MmUwZjI5ODQwNWJiMmNhODE2ZThiMjIzNTY3IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RIdMuv3kfzbNvllx9NC_AUCnntQ6k10DbjY7IFVmwzVx2Cf5OTOV0DA0-eBPBKZ-rpB-0--VirohiV9knqymdNWEKDhur6pNzdDoNRd-yTmni2Dq0dYxxliXcxPFS_6fT9b-jIxo2U9V9F_U9RXTSEP-9mCRglLXErnN-WMXIVMPsnWB44qOGWSSogoxavCWKFINu0OB4agMCmS6dOnr0xG41kx7yJ5e4imk5Cjd5Nttz7cKretZT1Ts6samCqQzol19S2wj9BcQ0RIk8pJ2QH-cURjcOVv0Fmd95y1dxjYR9FALhmaKJub2FdFRYWaJnOrCmPOdLfk9f4N7sppEJQ" | jq

# System: Increment unread count (called by message delivery service)
# 🔓 No auth required — internal service-to-service call
curl -X PATCH "http://127.0.0.1:8005/api/members/2d45f6c3-fbe5-4f69-a1af-30d4ce3124e2/unread?amount=5" \
  -H "X-Internal-Key: eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0NzU5LCJpYXQiOjE3Nzg1MjQ0NTksImp0aSI6Ijk4MTI4MmUwZjI5ODQwNWJiMmNhODE2ZThiMjIzNTY3IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RIdMuv3kfzbNvllx9NC_AUCnntQ6k10DbjY7IFVmwzVx2Cf5OTOV0DA0-eBPBKZ-rpB-0--VirohiV9knqymdNWEKDhur6pNzdDoNRd-yTmni2Dq0dYxxliXcxPFS_6fT9b-jIxo2U9V9F_U9RXTSEP-9mCRglLXErnN-WMXIVMPsnWB44qOGWSSogoxavCWKFINu0OB4agMCmS6dOnr0xG41kx7yJ5e4imk5Cjd5Nttz7cKretZT1Ts6samCqQzol19S2wj9BcQ0RIk8pJ2QH-cURjcOVv0Fmd95y1dxjYR9FALhmaKJub2FdFRYWaJnOrCmPOdLfk9f4N7sppEJQ" | jq



# List all active members of a room
curl -X GET http://127.0.0.1:8005/api/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" | jq

# Get a specific member by ID
curl -X GET http://127.0.0.1:8005/api/members/c98f4714-ae54-466e-8806-3dbbdb1a9802 \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" | jq

# Get MY membership in a room (convenience endpoint)
# 🔐 Uses authenticated user from JWT — no userId parameter needed
curl -X GET http://127.0.0.1:8005/api/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/me \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" | jq

# Check if a user is a member of a room (lightweight existence check)
# Returns 204 No Content if exists, 404 if not
curl -I -X GET http://127.0.0.1:8005/api/rooms/11111111-1111-1111-1111-111111111111/members/22222222-2222-2222-2222-222222222222 \
  --cookie "access_token=YOUR_ACCESS_TOKEN"































# =============================================================================
# MEMBER QUERY CONTROLLER - CURL TESTS
# Base URL: http://127.0.0.1:8005
# Auth: JWT via --cookie "access_token=..."
# Note: All endpoints return enriched MemberQueryResponseDTO with:
#   - user object (UserView from Auth Service)
#   - unread_messages, joined_at, updated_at, is_active metadata
# =============================================================================

# ─────────────────────────────────────────────────────────────────────────────
# SINGLE ENTITY QUERIES
# ─────────────────────────────────────────────────────────────────────────────

# Get a specific member by ID (enriched response)
curl -X GET http://127.0.0.1:8005/api/query/members/c98f4714-ae54-466e-8806-3dbbdb1a9802 \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Get membership for specific user in specific room
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/users/98787ef6-f118-400c-ad64-66e5634e664c \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq







# Get MY membership in a room (convenience endpoint - uses JWT user)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/me \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Check if user is active member of room (204 = exists, 404 = not found)
curl -I -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members/98787ef6-f118-400c-ad64-66e5634e664c \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng"

# Get member status in room (returns "ADMIN" or "USER" string)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/users/98787ef6-f118-400c-ad64-66e5634e664c/status \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Get MY unread count in room (for UI badge counters)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/me/unread \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwOTI2LCJpYXQiOjE3Nzg1MzA2MjYsImp0aSI6Ijg3ZWEzNDhhNDRiOTRjMTE5OGEzYzAyYzE1ZmY4NTA0IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.JQeSybFmPhl00QhrOsnVfBFY0kfp6ujp72mmEwbcGddEz5IHUnvNzob0JGjQhGHXE_S2ziteZMFt_cMc8UdmIxeA975HAnSerPTcCltzUskD2Q0J4lv9IwGnSNjMc8XQC_7HNTryMlG4Va7IK87jFDhpobEb5gAlep2IwSdPEM4JxiFuAMwgFjG5fyAd1AMAJ2IjwkULCmrWNK9X9tGP5eTHOowhHgSSrLbJv6ywtu7Hu6jhTL8LuVlGj6G7r4brQPDNPsRjrSazeFHPkmksbxbaldi1cmi9sds-NVxHLIBKmn4jU2go3_aWcUHi4fia2F92l2yqjJyb2lKi3v1-sA" \
  | jq


# ─────────────────────────────────────────────────────────────────────────────
# BULK QUERIES BY ROOM (Active Members Only)
# ─────────────────────────────────────────────────────────────────────────────

# List ALL active members of a room (enriched with user profiles)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  | jq

# List only ADMIN members of a room
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members/admins \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  | jq

# List only regular USER members of a room (excludes admins)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members/users \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  | jq

# Get count of active members in room (lightweight integer response)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members/count \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  | jq

# Get lightweight member summaries for room (no external UserView calls - faster)
curl -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members/summaries \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  | jq


# ─────────────────────────────────────────────────────────────────────────────
# BULK QUERIES BY USER (Active Memberships Only)
# ─────────────────────────────────────────────────────────────────────────────

# Get ALL my active memberships across all rooms (convenience endpoint)
curl -X GET http://127.0.0.1:8005/api/query/users/me/memberships \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Get memberships for a specific user (admin/privacy check may be needed)
curl -X GET http://127.0.0.1:8005/api/query/users/98787ef6-f118-400c-ad64-66e5634e664c/memberships \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Get my ADMIN-only memberships (rooms I administer)
curl -X GET http://127.0.0.1:8005/api/query/users/me/memberships/admin \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Get count of my active memberships (lightweight integer)
curl -X GET http://127.0.0.1:8005/api/query/users/me/memberships/count \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq

# Get my membership summaries (lightweight, no external UserView calls)
curl -X GET http://127.0.0.1:8005/api/query/users/me/memberships/summaries \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  | jq


# ─────────────────────────────────────────────────────────────────────────────
# BATCH LOOKUP QUERIES (POST with JSON body)
# ─────────────────────────────────────────────────────────────────────────────

# Bulk fetch members by user IDs within a specific room
# Request body: JSON array of UUID strings
curl -X POST http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/members/bulk \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  -d '[
    "98787ef6-f118-400c-ad64-66e5634e664c",
    "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
    "11111111-1111-1111-1111-111111111111"
  ]' \
  | jq

# Bulk fetch members by member IDs
# Request body: JSON array of UUID strings
curl -X POST http://127.0.0.1:8005/api/query/members/bulk \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2OTAxLCJpYXQiOjE3Nzg1MjY2MDEsImp0aSI6IjRkMDE5NjhiYmFjMTRjMmRhYjY0YzRlMzE0Zjc4NGRkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RT4TKbi_ZVRpj2UnxkoP7FMSMReQYswFh7vNM55VIUWZMVX9R-zss2hcz4ur5iIdvm0AN0csMV63eDT5pGO-7hMfIjTrVhFjHWSUOlWnTeOCBEwHqir6wdFNwVdexiPGrM1FTqZyXY_4BIRzPGop0mgZsss-dsBC_ixYLF2adTpToZFYCfFVOzThYE3gBdiGVJHAlSswDmTwtddsB9y9HUnGiOLrqEJpkLfH0oWWXJk6cj2IYP-DHOw8Id2568gW6gNhJVScDE5t5MjekXC5Ho_9COXMPoqHlTNuIQijPuq7WYgIjLT4ei4TWENFIDM8k0qhP3A1AxF7v_FzJFltfQ" \
  -d '[
    "c98f4714-ae54-466e-8806-3dbbdb1a9802",
    "2d45f6c3-fbe5-4f69-a1af-30d4ce3124e2"
  ]' \
  | jq


# ─────────────────────────────────────────────────────────────────────────────
# EXISTENCE CHECKS (204 = exists, 404 = not found)
# ─────────────────────────────────────────────────────────────────────────────

# Check if member exists by member ID
curl -I -X GET http://127.0.0.1:8005/api/query/members/c98f4714-ae54-466e-8806-3dbbdb1a9802/exists \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng"

# Check if MY membership exists in room (convenience endpoint)
curl -I -X GET http://127.0.0.1:8005/api/query/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/me/exists \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng"
