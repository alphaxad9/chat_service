

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
    "identifier": "brocode2",
    "password": "2025New+!"
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
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjU5NjQxLCJpYXQiOjE3Nzg2NTkzNDEsImp0aSI6IjFmZDA1YjFmOGMyNDQ0NWE4NjJkNDNjMGNjNzJiNDRiIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.DhfaGmDHGEsVjYR3ajt8HdZb6lh09GwfznfXTOAQJxYtamzMnwz98At_htJAifHqg3g-2KQbtZ2kt6FkdVN6ZEeS9ZrMtF5OzdTkmiyOjj7Y9B50HEDcZFyfX7DGe_4Y_bJUkO1yXs07IbpRXKAoFH7U39ADfpZElqHqXxDwXHq19daiTaljC2672lHclkt7omi_AAQ_BqQ_vt-g4xsXv2TPC-VI6cMB3zdmNUiNLFlHmFDtSdt7oP2jX7aBEZB0mPQuKy2Lt6e320o7uhQ8azCW8_jS56X4onVA5vMmo2J3PaAirzoF63c0NaiqmwsRgK4nPhNc6TMm0bPUGNQ80w" \
  -F "group_name=Project Alpha" \
  -F "description=Collaboration space for Project Alpha team" \
  -F 'participant_ids=[  "98787ef6-f118-400c-ad64-66e5634e664c",
  "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
  "f65d68f6-3973-43c0-b776-c2b8cf38e0a4",
  "71885bbe-1f48-42b6-90e7-f988af5231dd"]' \
  -F "profile_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" | jq

# Create a GROUP room with BOTH profile image AND cover image
curl -X POST http://127.0.0.1:8005/api/rooms/groups \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjYxNTMwLCJpYXQiOjE3Nzg2NjEyMzAsImp0aSI6IjI1NGUzYjE3ZTRiNTQzZGJiZDI4ZDg1MWY0YTE1OTM2IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.bjVpvTZYgHw5lrKAI5AvhyhvXDYJ-XQcl2KOa8eTLNIHRB45oXmoyHxRABtZp4C0Lw9u_C988wQGLdWbLRcOoLBu4mwXU9FLE6HypCcXLhAGSROa_MoBfnMUYjUXxxS7KzT9yPxw6DPiFHoRt6ZaZWfVrwbxzxWpL6UWq3t09N6t__cD1z-MvTnzlPBsMhHexR7g6If9qwWfNU1gaNBvaSeajufueg3XzZAiNXdBc24ZYhsNb29607XGYIowIHU5OiMVkctTkahUwxdkg_h9ihYWBHaAmiCvJ8cCTKqejOvDoXzYV7I0MgtBQx7zGER-Hp6_gSDITaWdCjv_ArHWMw" \
  -F "group_name=Marketing2 Teame" \
  -F "description=Marketing campaigns and assets" \
  -F 'participant_ids=[  "98787ef6-f118-400c-ad64-66e5634e664c",
  "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
  "f65d68f6-3973-43c0-b776-c2b8cf38e0a4",
  "71885bbe-1f48-42b6-90e7-f988af5231dd"]' \
  -F "profile_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" \
  -F "cover_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq
# ───────────────────────────────────────────────────────────────────────────
# 2. CREATE DIRECT ROOM (application/json)
# ───────────────────────────────────────────────────────────────────────────

# Create new DIRECT room between authenticated user and friend
# 🔐 Authenticated user is creator; friend_id is the other participant
curl -X POST http://127.0.0.1:8005/api/rooms/direct \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg0NDEwLCJpYXQiOjE3Nzg1ODQxMTAsImp0aSI6ImU2NGNmYTI3NDhjOTQwNjNiZmNmOGY0M2NlOTUwZGE2IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.PXug6dV7h0ooEfhM4QYuxKpUJXA7HWO_hd2FPDwmN3xzIdq0V46Fu1NzBDnARbj_SOgggYKFhEqS0pbLvrJjunRPuoDKZWKUXeGwXXjOjZ6uMnX97Rkb-OjLm6N1H6-p3b-60T91H0mWwlN4ApqoRuN3aJ_FKJCUeac2ICfWpF2Sb5iM40vprj51KtOPUn_wplDWYu28Hc0fKW9QEE2MlhO6A7_880Ue9OXiqA0zAWZbELjbWgsTRVaaMRDJXwZ6L7pu2HNnx6S7OEXVpdhdyat1ZHRfW26NGLN5H5OcFg-mZyyfThvqRYoUfKcdYpRvw83Qv6f4-UmbrdCce7hE6w" \
  -d '{
    "friend_id": "9e6c4138-3129-4875-8e72-25e4cb05905d"
  }' | jq

# Test BIDIRECTIONAL DEDUPLICATION: Create same DIRECT room from friend's perspective
# Should return EXISTING room (200 OK or 201 with same room_id), not create duplicate
curl -X POST http://127.0.0.1:8005/api/rooms/direct \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg1NTMzLCJpYXQiOjE3Nzg1ODUyMzMsImp0aSI6IjNjZDZlMzc1Yzg3NTQ5MTdiNDExMjY2NjZhNzMwZDRkIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.ALrFM5Zo1lxN9kB4WstPR5hV9iIKna_wh0-hKJxVH2JF4FnIQZz5D-CclPSu_xgAl2mfYQ_OUrQU1CFr8hMaWUWd3VX4ImtJtXxY1fkUufnAELqViTTUrl7X8EOc1jZtzpEbncZNdJS9kvc4wJqu5QhZV_eRmQFY7CGsDacAKsbJfqGAk7ZIpSPUmJpfo0DiMif_ei9kXoELaEXcRE_P6U1rOxgSXkWRmL0MkGDiBlwtQy4MzCGU8pZB7qImYU9UKZWkfbZOi-VnkOUN_Sc95sh2foXbZV4ep-jV6fEIVegn5SdDYUXfTJ7IU7lPhZCjeXf9SYyaI9SW8gfHsnqLYw" \
  -d '{
    "friend_id": "71885bbe-1f48-42b6-90e7-f988af5231dd"
  }' | jq

# Create DIRECT room with different friend
curl -X POST http://127.0.0.1:8005/api/rooms/direct \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjU2NTIyLCJpYXQiOjE3Nzg2NTYyMjIsImp0aSI6IjM3ZWMxYzY4Y2M4NzQwY2E4MzdkNDk1ZjQ5ODVjMThlIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.p7ADslJFYZgBVYel-KyDmHJNwQVz51O30xVZvv2svDmsrVuWnId4vZimUfjPV-YNgc7WgVoebXVPzP__ZDaRQ5FMkNvQNCpWonFWnVF-OrXZIgXxJrDnxxK85V6mdTyatBoLZo2kVA_M_kKFhRGcshwXb4JysGQ0jsoesje-rgcV8dJQ8-siEmc1OHXKkKsjCOXNg5PhOoFb8ZUd9SzsjlbQbXNFFM8FuIdx0mcOZTjSnU1jZSLAdpFKObumGLxJXLAo4B9XguoPlairrQGaBisXIEdGfH5IlBy8H5-6OayS4O4wTbOsNh7o6rBwa_QMdyO-BQnB8n66wILSDfWDAg" \
  -d '{
    "friend_id": "0beaf05f-3f45-466f-8913-9f218b0d7884"
  }' | jq


# =============================================================================
# ROOM COMMAND API - UPDATE ACTIONS CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace {room_id} with an actual GROUP room UUID from a previous creation response
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 3. UPDATE GROUP ROOM ACTIONS (return GroupUpdateActionsResponse)
# ───────────────────────────────────────────────────────────────────────────

# ── DELETE GROUP ROOM ──────────────────────────────────────────────────────

# Delete a GROUP room (only creator can delete)
# 🔐 Authenticated user must be the room creator
# Returns final room state for UI sync before navigation
curl -X DELETE http://127.0.0.1:8005/api/rooms/groups/16f2d91b-f878-4087-87bc-87ea15b5f813 \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg4NjQ2LCJpYXQiOjE3Nzg1ODgzNDYsImp0aSI6ImRiNDU1OTEyYTE5ZDQ3MGFhMGQxN2UyMzFiNGRjMDg4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.G-8hNcEuypkl7ff4nCdyNw1wgPb2VD_7qxIG2KXrJ0ZYMb1BFTFMUit4QmsBCv_47UCpj-EewPt854998K6QKAvizVBJIP29Ir8u5ok3rH5pT_kIOZhHFHRLIE-e80JlIR8GNrJTklZIxjSCZW_M61Ixn5ZQCxMDGB14Wd6ZGoVIft01wVnXx2-e_FyzMK_SK-o8CuEAwQfQahMYVkrT0mgEeH98yxJ9Le5Ggrs3wnhTIKQkq5t1Cq76XwCKwSesJtPqdmMqJGEf63-xbmdQAMNiUAv3WCC6XVIH-vm8EWKV6cGXNBgfExBRKv_-9IuJjma8j5MNWaI252Ij7pHdEA" \
  | jq


# ── UPDATE GROUP NAME ─────────────────────────────────────────────────────

# Update the group name (JSON body)
# 🔐 Only room creator/admin can update
curl -X PATCH http://127.0.0.1:8005/api/rooms/groups/fbe31df8-6136-4ff6-bf6d-3b0f15164270/name \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg4NjQ2LCJpYXQiOjE3Nzg1ODgzNDYsImp0aSI6ImRiNDU1OTEyYTE5ZDQ3MGFhMGQxN2UyMzFiNGRjMDg4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.G-8hNcEuypkl7ff4nCdyNw1wgPb2VD_7qxIG2KXrJ0ZYMb1BFTFMUit4QmsBCv_47UCpj-EewPt854998K6QKAvizVBJIP29Ir8u5ok3rH5pT_kIOZhHFHRLIE-e80JlIR8GNrJTklZIxjSCZW_M61Ixn5ZQCxMDGB14Wd6ZGoVIft01wVnXx2-e_FyzMK_SK-o8CuEAwQfQahMYVkrT0mgEeH98yxJ9Le5Ggrs3wnhTIKQkq5t1Cq76XwCKwSesJtPqdmMqJGEf63-xbmdQAMNiUAv3WCC6XVIH-vm8EWKV6cGXNBgfExBRKv_-9IuJjma8j5MNWaI252Ij7pHdEA" \
  -d '{
    "new_name": "Project Alpha - Updated"
  }' | jq



# ── UPDATE GROUP DESCRIPTION ──────────────────────────────────────────────

# Update the room description (JSON body)
# Pass empty string "" to clear the description
curl -X PATCH http://127.0.0.1:8005/api/rooms/groups/fbe31df8-6136-4ff6-bf6d-3b0f15164270/description \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg4NjQ2LCJpYXQiOjE3Nzg1ODgzNDYsImp0aSI6ImRiNDU1OTEyYTE5ZDQ3MGFhMGQxN2UyMzFiNGRjMDg4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.G-8hNcEuypkl7ff4nCdyNw1wgPb2VD_7qxIG2KXrJ0ZYMb1BFTFMUit4QmsBCv_47UCpj-EewPt854998K6QKAvizVBJIP29Ir8u5ok3rH5pT_kIOZhHFHRLIE-e80JlIR8GNrJTklZIxjSCZW_M61Ixn5ZQCxMDGB14Wd6ZGoVIft01wVnXx2-e_FyzMK_SK-o8CuEAwQfQahMYVkrT0mgEeH98yxJ9Le5Ggrs3wnhTIKQkq5t1Cq76XwCKwSesJtPqdmMqJGEf63-xbmdQAMNiUAv3WCC6XVIH-vm8EWKV6cGXNBgfExBRKv_-9IuJjma8j5MNWaI252Ij7pHdEA" \
  -d '{
    "new_description": "Updated description for Project Alpha collaboration space. Now includes design assets and sprint planning."
  }' | jq

# Clear the description by sending empty string
curl -X PATCH http://127.0.0.1:8005/api/rooms/groups/{room_id}/description \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg1NTMzLCJpYXQiOjE3Nzg1ODUyMzMsImp0aSI6IjNjZDZlMzc1Yzg3NTQ5MTdiNDExMjY2NjZhNzMwZDRkIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.ALrFM5Zo1lxN9kB4WstPR5hV9iIKna_wh0-hKJxVH2JF4FnIQZz5D-CclPSu_xgAl2mfYQ_OUrQU1CFr8hMaWUWd3VX4ImtJtXxY1fkUufnAELqViTTUrl7X8EOc1jZtzpEbncZNdJS9kvc4wJqu5QhZV_eRmQFY7CGsDacAKsbJfqGAk7ZIpSPUmJpfo0DiMif_ei9kXoELaEXcRE_P6U1rOxgSXkWRmL0MkGDiBlwtQy4MzCGU8pZB7qImYU9UKZWkfbZOi-VnkOUN_Sc95sh2foXbZV4ep-jV6fEIVegn5SdDYUXfTJ7IU7lPhZCjeXf9SYyaI9SW8gfHsnqLYw" \
  -d '{
    "new_description": ""
  }' | jq

# ── UPDATE COVER IMAGE (multipart/form-data) ──────────────────────────────

# Update cover image with new file upload
# 🔐 Only room creator/admin can update
curl -X PATCH http://127.0.0.1:8005/api/rooms/groups/fbe31df8-6136-4ff6-bf6d-3b0f15164270/cover-image \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg4NjQ2LCJpYXQiOjE3Nzg1ODgzNDYsImp0aSI6ImRiNDU1OTEyYTE5ZDQ3MGFhMGQxN2UyMzFiNGRjMDg4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.G-8hNcEuypkl7ff4nCdyNw1wgPb2VD_7qxIG2KXrJ0ZYMb1BFTFMUit4QmsBCv_47UCpj-EewPt854998K6QKAvizVBJIP29Ir8u5ok3rH5pT_kIOZhHFHRLIE-e80JlIR8GNrJTklZIxjSCZW_M61Ixn5ZQCxMDGB14Wd6ZGoVIft01wVnXx2-e_FyzMK_SK-o8CuEAwQfQahMYVkrT0mgEeH98yxJ9Le5Ggrs3wnhTIKQkq5t1Cq76XwCKwSesJtPqdmMqJGEf63-xbmdQAMNiUAv3WCC6XVIH-vm8EWKV6cGXNBgfExBRKv_-9IuJjma8j5MNWaI252Ij7pHdEA" \
  -F "cover_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq

# Remove/clear the cover image using ?remove=true query param
curl -X PATCH "http://127.0.0.1:8005/api/rooms/groups/{room_id}/cover-image?remove=true" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg1NTMzLCJpYXQiOjE3Nzg1ODUyMzMsImp0aSI6IjNjZDZlMzc1Yzg3NTQ5MTdiNDExMjY2NjZhNzMwZDRkIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.ALrFM5Zo1lxN9kB4WstPR5hV9iIKna_wh0-hKJxVH2JF4FnIQZz5D-CclPSu_xgAl2mfYQ_OUrQU1CFr8hMaWUWd3VX4ImtJtXxY1fkUufnAELqViTTUrl7X8EOc1jZtzpEbncZNdJS9kvc4wJqu5QhZV_eRmQFY7CGsDacAKsbJfqGAk7ZIpSPUmJpfo0DiMif_ei9kXoELaEXcRE_P6U1rOxgSXkWRmL0MkGDiBlwtQy4MzCGU8pZB7qImYU9UKZWkfbZOi-VnkOUN_Sc95sh2foXbZV4ep-jV6fEIVegn5SdDYUXfTJ7IU7lPhZCjeXf9SYyaI9SW8gfHsnqLYw" \
  | jq

# ── UPDATE PROFILE IMAGE (multipart/form-data) ────────────────────────────

# Update profile image with new file upload
curl -X PATCH http://127.0.0.1:8005/api/rooms/groups/fbe31df8-6136-4ff6-bf6d-3b0f15164270/profile-image \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg4NjQ2LCJpYXQiOjE3Nzg1ODgzNDYsImp0aSI6ImRiNDU1OTEyYTE5ZDQ3MGFhMGQxN2UyMzFiNGRjMDg4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.G-8hNcEuypkl7ff4nCdyNw1wgPb2VD_7qxIG2KXrJ0ZYMb1BFTFMUit4QmsBCv_47UCpj-EewPt854998K6QKAvizVBJIP29Ir8u5ok3rH5pT_kIOZhHFHRLIE-e80JlIR8GNrJTklZIxjSCZW_M61Ixn5ZQCxMDGB14Wd6ZGoVIft01wVnXx2-e_FyzMK_SK-o8CuEAwQfQahMYVkrT0mgEeH98yxJ9Le5Ggrs3wnhTIKQkq5t1Cq76XwCKwSesJtPqdmMqJGEf63-xbmdQAMNiUAv3WCC6XVIH-vm8EWKV6cGXNBgfExBRKv_-9IuJjma8j5MNWaI252Ij7pHdEA" \
  -F "profile_image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" | jq

# Remove/clear the profile image using ?remove=true query param
curl -X PATCH "http://127.0.0.1:8005/api/rooms/groups/{room_id}/profile-image?remove=true" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg1NTMzLCJpYXQiOjE3Nzg1ODUyMzMsImp0aSI6IjNjZDZlMzc1Yzg3NTQ5MTdiNDExMjY2NjZhNzMwZDRkIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.ALrFM5Zo1lxN9kB4WstPR5hV9iIKna_wh0-hKJxVH2JF4FnIQZz5D-CclPSu_xgAl2mfYQ_OUrQU1CFr8hMaWUWd3VX4ImtJtXxY1fkUufnAELqViTTUrl7X8EOc1jZtzpEbncZNdJS9kvc4wJqu5QhZV_eRmQFY7CGsDacAKsbJfqGAk7ZIpSPUmJpfo0DiMif_ei9kXoELaEXcRE_P6U1rOxgSXkWRmL0MkGDiBlwtQy4MzCGU8pZB7qImYU9UKZWkfbZOi-VnkOUN_Sc95sh2foXbZV4ep-jV6fEIVegn5SdDYUXfTJ7IU7lPhZCjeXf9SYyaI9SW8gfHsnqLYw" \
  | jq

















# =============================================================================
# ROOM QUERY API - CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# 🔑 Backend invariant: Only rooms with at least one active message are returned
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 4. QUERY ROOMS FOR USER HOME PAGE (GET /api/query/rooms/home)
# ───────────────────────────────────────────────────────────────────────────

# Fetch authenticated user's room list for home page display
# 🔐 User ID extracted from JWT token in UserContext
# ✅ Returns: List<MyRoomsHomePageListDto> with absolute image URLs
# ✅ Backend invariant: Only rooms with at least one active message included
# ✅ Room ordering: last_activity_at descending (most recent first)
# ✅ DIRECT rooms: name=friend's username, profile_image=friend's profile pic
# ✅ GROUP rooms: name=group name, profile_image=room's profile pic
# ✅ Last message: image-over-text priority, "You" personalization when is_mine=true
curl -X GET http://127.0.0.1:8005/api/query/rooms/home \
  -H "Accept: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjYxOTg0LCJpYXQiOjE3Nzg2NjE2ODQsImp0aSI6IjU0NTEwYmRlN2Q0YzQwNWNiNDIyZjA0M2ZhNzU1NWQ1IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.uXhkeoqt1Xg62NSuuxCq9EXlw514aDAPzbAbOztyMkG4tKp1O6T3PBHtleHaMGlFyPOfazERFldmCguQQ2EPXoR-mvVWsMr3iDP2lqFABQ7O_Ck9JvJBjW9T5tvqCAhqkKkhUq-tRjW8DkTY0RSGnCsErtD6iPx37me_26MjuO5wwg5WkRiU30dfSzrjClbZae55nBe9XzKmfXgvMbsfiMcuWmqvbeamZMFdmPEFBFRKnxJjoBh2KoBLRCHi6aKFTnQyq-4QNHky5M4TJBJ7C9qYcVRE2BwWbjuhxOuK8E3kqHkisChFWrLlVgw352ehV7UmspQNWG21xmGishEUDg" \
  | jq

 



# =============================================================================
# ROOM QUERY API - SINGLE ROOM DETAIL CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace {room_id} with an actual room UUID from previous creation responses
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 5. GET SINGLE ROOM BY ID (for room detail/settings view)
# ───────────────────────────────────────────────────────────────────────────

# Fetch details for a GROUP room (authenticated user must be a member)
# 🔐 Returns 404 if user is not a member or room doesn't exist
# ✅ Response includes: name, profile/cover images, description, is_admin, is_owner, timestamps
curl -X GET http://127.0.0.1:8005/api/query/rooms/baac4bda-9a74-491a-9241-b16ec1914cd2 \
  -H "Accept: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjU5NjQxLCJpYXQiOjE3Nzg2NTkzNDEsImp0aSI6IjFmZDA1YjFmOGMyNDQ0NWE4NjJkNDNjMGNjNzJiNDRiIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.DhfaGmDHGEsVjYR3ajt8HdZb6lh09GwfznfXTOAQJxYtamzMnwz98At_htJAifHqg3g-2KQbtZ2kt6FkdVN6ZEeS9ZrMtF5OzdTkmiyOjj7Y9B50HEDcZFyfX7DGe_4Y_bJUkO1yXs07IbpRXKAoFH7U39ADfpZElqHqXxDwXHq19daiTaljC2672lHclkt7omi_AAQ_BqQ_vt-g4xsXv2TPC-VI6cMB3zdmNUiNLFlHmFDtSdt7oP2jX7aBEZB0mPQuKy2Lt6e320o7uhQ8azCW8_jS56X4onVA5vMmo2J3PaAirzoF63c0NaiqmwsRgK4nPhNc6TMm0bPUGNQ80w" \
  | jq















# =============================================================================
# ROOM QUERY API - USERS FOR NEW CONVERSATION CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 6. GET USERS FOR STARTING NEW CONVERSATION
# ───────────────────────────────────────────────────────────────────────────

# Fetch users for conversation starters with default pagination (limit=20, offset=0)
# ✅ Returns: Auth Service users + friends from empty DIRECT rooms (deduplicated, excludes self)
curl -X GET "http://127.0.0.1:8005/api/query/rooms/users-for-conversation" \
  -H "Accept: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjYwOTU3LCJpYXQiOjE3Nzg2NjA2NTcsImp0aSI6ImM0YTAwZmVjYTM2ODQ2MzhiOGZlOWM3YWU2ODBiYTVkIiwidXNlcl9pZCI6IjBiZWFmMDVmLTNmNDUtNDY2Zi04OTEzLTlmMjE4YjBkNzg4NCJ9.dOCn9scpo2ZJxcdrMcG-atlX-OAJ941P8mQ1ZEqPZvc2_3mJiYBNZtvyjf1Cm2K_ychbnibOp9KnAPrVkWW0_jshCR_m-HuiWzt5cuMR4Q0DHNE9Ntk6E1EB2v3xD3HG2JvCFv9uSVYEo5fge6hHYSNhRtRx3KMGTyTZf6iISaVKVwwkpXhTlX9VTkhl8qj-xMIPzhbops9ot4mhnN-wxF3kb89jGejsmDpyoF4Ll_EcWOtGS6J4RZEurRIYu47u_e0-jCP6mulcM_8HwPGDmbmJODlKIHL4t_sRMwi_YXjnEN6Som6HeVYvdNlvtKT6OT-Kn5qfLY5X2D8VJkwQaQ" \
  | jq

