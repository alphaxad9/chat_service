users=[
  [
  "9e6c4138-3129-4875-8e72-25e4cb05905d",
  "012a181c-0c14-4aba-b868-4329555c3540",
  "5eab88a4-d9ae-48ab-b0c9-5e66d4c49b17",
  "5cc497fa-74e9-41cc-b4a2-d68977bc0ad7",
  "5c97a647-5dff-43b5-bef1-911e4c217dce",
  "a0203f44-4fa6-4dba-b7a8-ac2e85d56b15",
  "0beaf05f-3f45-466f-8913-9f218b0d7884",
  "98787ef6-f118-400c-ad64-66e5634e664c",
  "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
  "f65d68f6-3973-43c0-b776-c2b8cf38e0a4",
  "71885bbe-1f48-42b6-90e7-f988af5231dd"
]
]

curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test9@example.com",
    "password": "Test123!"
  }' | jq

curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test@example.com",
    "password": "Test123!"
  }' | jq



# =============================================================================
# ROOM COMMAND API - CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..." or Authorization header
# All responses piped through jq for pretty JSON formatting
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 1. CREATE GROUP ROOM (multipart/form-data)
# ───────────────────────────────────────────────────────────────────────────

# Create a GROUP room with 2 participants + profile image
# 🔐 Authenticated user becomes ADMIN/creator automatically
curl -X POST http://127.0.0.1:8005/api/rooms/groups \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  -F "group_name=Project Alpha" \
  -F "description=Collaboration space for Project Alpha team" \
  -F 'participant_ids=[  "98787ef6-f118-400c-ad64-66e5634e664c",
  "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
  "f65d68f6-3973-43c0-b776-c2b8cf38e0a4",
  "71885bbe-1f48-42b6-90e7-f988af5231dd"]' \
  -F "profile_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" | jq

# Create a GROUP room with BOTH profile image AND cover image
curl -X POST http://127.0.0.1:8005/api/rooms/groups \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  -F "group_name=Marketing Team" \
  -F "description=Marketing campaigns and assets" \
  -F 'participant_ids=["5eab88a4-d9ae-48ab-b0c9-5e66d4c49b17","5cc497fa-74e9-41cc-b4a2-d68977bc0ad7","5c97a647-5dff-43b5-bef1-911e4c217dce"]' \
  -F "profile_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" \
  -F "cover_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq
# ───────────────────────────────────────────────────────────────────────────
# 2. CREATE DIRECT ROOM (application/json)
# ───────────────────────────────────────────────────────────────────────────

# Create new DIRECT room between authenticated user and friend
# 🔐 Authenticated user is creator; friend_id is the other participant
curl -X POST http://127.0.0.1:8005/api/rooms/direct \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTgzNTUwLCJpYXQiOjE3Nzg1ODMyNTAsImp0aSI6ImEzMjU4NGRmYWM5MDQyNDRiNmM1Y2NkMGFhYWUyMDA0IiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.J7W2TJVZ_EsH5g5n4FyHX8ENZcXfklB_YE-E-FDIr2Q1f339kmZwJX8XglRxx3atG2HSLqBzI6UFrwuuewhsFEmC97zoBsw9dmODFsWBgKQKwzplV3Hq5k7qtGJprPj7zZ32WAu6NQczXr6M5-GS4XWeZNaR7nL5rHCKnMkGt5QVcIgyYmui1MYkermn8HqPlf9LPABmuMzFIWdOZHH2fgj2ePOMo5Lls1t5rA4S3N2a1XvMHbGtN_DciXV-3oWAywGmxQxFKHEGDc_PGf2LPJ8guIoKbFSq2gSdREa0rBiSjo2i7jg2VvXwRpvxc1Dy_dVLDYiZTpNAbUf0WoQx6Q" \
  -d '{
    "friend_id": "71885bbe-1f48-42b6-90e7-f988af5231dd"
  }' | jq

# Test BIDIRECTIONAL DEDUPLICATION: Create same DIRECT room from friend's perspective
# Should return EXISTING room (200 OK or 201 with same room_id), not create duplicate
curl -X POST http://127.0.0.1:8005/api/rooms/direct \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6ImIyYzNkNGU1LWY2YTctODkwMS1iY2RlLWYxMjM0NTY3ODkwMSJ9.DIFFERENT_TOKEN_FOR_FRIEND_USER" \
  -d '{
    "friend_id": "71885bbe-1f48-42b6-90e7-f988af5231dd"
  }' | jq

# Create DIRECT room with different friend
curl -X POST http://127.0.0.1:8005/api/rooms/direct \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMwNjc3LCJpYXQiOjE3Nzg1MzAzNzcsImp0aSI6IjczZmRlODFiZWUzNzRhN2U4MGFjZjViY2Q3YjM5ZmQ4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.TRbtQQaISwiIMQR-CoFto0Ixd05_wxMVjwBdpzStySyCfYoZO1TrUwDoz6094XoPe5YMi6m-CoJ7BLikpboF1XL-IX1dUOvKOrX5NDXGjLaEGlF5be0tpiiaZeqSc4Sp-kvAkE22Tfyrf1YbzZbXoEOlu8RDB4C9SysJvOIBUg5IO2mVI4g1NAr2Lhlosty-UgZXxvz1wGUchbZXj_xzbMM9FiRkQAut-zGn8Up_L5V4jiHMTC8wx1x-wInqqHU85eYk8GDnBJbgf3L9ngj4oXMmQ97cQ85GEHwqbcHYQydf_p2LIndlDdrgp1NVJlXniz_H9NmrkyYGxYOT3CGNng" \
  -d '{
    "friend_id": "0beaf05f-3f45-466f-8913-9f218b0d7884"
  }' | jq

