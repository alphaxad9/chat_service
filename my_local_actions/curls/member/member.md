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
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0MzM5LCJpYXQiOjE3Nzg1MjQwMzksImp0aSI6IjA1NThhZWI4ZDZkMDQwNTI5YzBkNDljZDk4Mjk4OTViIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.LtCqhlsJVRawy6ka44E21TztpQcZotsJmnTr_XxPdlbrhThgbpyynLsKGuWzCzhrGxXBgkj3hED-gGxldz0BdjA7jVGlN4CNAdIEjJJkGwwP5YIA9aSXm3dt_zhu1OU3YPscW8IayZf7k0xeO8vyJL2QAmwaKjPRp0IFu5_3vyD-oKsgJ-jsh5BcraoHOgqER9KtYXQOuC3LOyHhHSBrS_cTklg6k1PkxTjAGYfsn7F1CzpfZ6LeVTOHYl-HfbEBscnTB6OvsETUL0p1RswZnoq9jJRR30j4EbyF4qTz07PSY1vRP4zeACcIPMoXRgpJ-C-MbWod2dr22cQuDsBAnw" | jq

# Get MY membership in a room (convenience endpoint)
# 🔐 Uses authenticated user from JWT — no userId parameter needed
curl -X GET http://127.0.0.1:8005/api/rooms/71885bbe-3f48-42b6-90e7-f988af5231dd/me \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI0MzM5LCJpYXQiOjE3Nzg1MjQwMzksImp0aSI6IjA1NThhZWI4ZDZkMDQwNTI5YzBkNDljZDk4Mjk4OTViIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.LtCqhlsJVRawy6ka44E21TztpQcZotsJmnTr_XxPdlbrhThgbpyynLsKGuWzCzhrGxXBgkj3hED-gGxldz0BdjA7jVGlN4CNAdIEjJJkGwwP5YIA9aSXm3dt_zhu1OU3YPscW8IayZf7k0xeO8vyJL2QAmwaKjPRp0IFu5_3vyD-oKsgJ-jsh5BcraoHOgqER9KtYXQOuC3LOyHhHSBrS_cTklg6k1PkxTjAGYfsn7F1CzpfZ6LeVTOHYl-HfbEBscnTB6OvsETUL0p1RswZnoq9jJRR30j4EbyF4qTz07PSY1vRP4zeACcIPMoXRgpJ-C-MbWod2dr22cQuDsBAnw" | jq

# Check if a user is a member of a room (lightweight existence check)
# Returns 204 No Content if exists, 404 if not
curl -I -X GET http://127.0.0.1:8005/api/rooms/11111111-1111-1111-1111-111111111111/members/22222222-2222-2222-2222-222222222222 \
  --cookie "access_token=YOUR_ACCESS_TOKEN"