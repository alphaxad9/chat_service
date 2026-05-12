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
# MESSAGE COMMAND API - CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace {room_id} with an actual room UUID from a previous room creation
# 🔑 Replace {parent_id} with an actual message UUID from a previous message send
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# 
# Sample user IDs for testing:
#   "9e6c4138-3129-4875-8e72-25e4cb05905d"  # test9@example.com
#   "71885bbe-1f48-42b6-90e7-f988af5231dd"  # test@example.com
#   "0beaf05f-3f45-466f-8913-9f218b0d7884"  # another test user
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 1. SEND TEXT MESSAGE (application/json)
# ───────────────────────────────────────────────────────────────────────────

# Send a simple text message to an existing room
# 🔐 Authenticated user becomes the message sender automatically
# Response includes: message data, sender info, is_mine flag, status
curl -X POST http://127.0.0.1:8005/api/messages \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEzMjY0LCJpYXQiOjE3Nzg2MTI5NjQsImp0aSI6IjlmMTljM2Y4NDcwMDQ4MTBiYmViZjE5MmRiY2ZiYTc4IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.MmIWkxwnYiJ3o-k1oGCyWJMqzGWA-PPr5Yj-iu6IscvNaLYsNeMRvKQ8_oOZt9Gq8zHDyeUYqrrjmmhkd6md_EYiEw-hU0tXP2WxT1472n8_RuO_a_Jen2cGge4U0GCjhbNumfmrmuWvnmmYoYFIkvJw_W0nrgsdq-IdmR7_FW4A1Js5HmtQJHnMxmAh8z5beQppU7XZiBtgna5c4x597SHRbPZ3ZTZSSrCwch6Lwa-ygJmvEsDT-KRJUr_mLfbCBimLt_nEFBOzg0zDYRbGCGYPsvn0JTI_FCnjyUs2fNjDIxQASB2L9kgtV2EyEImsd1tYkoAOTn-0FtVBiM726g" \
  -d '{
    "room_id": "ca9ee906-a23a-4e07-9cb5-08684cf3b21a",
    "content": "Hello team! Starting the sprint planning session."
  }' | jq

# Send another text message from a different user to test unread count increment
curl -X POST http://127.0.0.1:8005/api/messages \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA3Mjg5LCJpYXQiOjE3Nzg2MDY5ODksImp0aSI6ImVlY2M5ZDNiYjMwZTQ1YjM4OTVjZTFmNGM5ZmZkODMxIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.u0Q_tX-LdjJG0qHZW5mp-eM9tjNZqoDs64j_VWNwzGPGVC_yerWHsW-rzW4f0JP_po8ytAHl0XvHtURldR36LT3dwkysm_nvUwiGaN2v6Brh1x8WcJfMWAPRMw-8UXDXrsrEk4uJAupmzIjV3uKtNNeZpiiqsNvwLNFzqTrWFVHEnhQLBWU0WhqYhmlSe3A3Cn4vDyvORAemFGwkJkClJbOia33KbuYNOk3xG-V00YovS65KGN1AJJczEh5CBjKHKzn-9UsajT4oXLjBnHrTmXrMnunzwI6jd4jve5PSUYFEsACy57g-WywmcVnV1-0jdEmviV3gvK-CtQr_6c4ZXw" \
  -d '{
    "room_id": "ca9ee906-a23a-4e07-9cb5-08684cf3b21a",
    "content": "Thanks for the update! I have a few questions about the timeline."
  }' | jq

# ───────────────────────────────────────────────────────────────────────────
# 2. SEND MESSAGE WITH IMAGE (multipart/form-data)
# ───────────────────────────────────────────────────────────────────────────

# Send a message with an image attachment
# 🔐 Authenticated user becomes sender; image saved to /uploads/messages/
# Response includes absolute image_url after MediaUrlService conversion
curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjE3MDUwLCJpYXQiOjE3Nzg2MTY3NTAsImp0aSI6IjVjNTRiMGExMzkyZTRmOTFiOGE3ZjQ0NTIzZDYzYWE2IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.tIZW0NNb5h8cj2VEZKdROy669O_bJTiuDTa3exeUzu9W3TT0PGP8lOwmKcPFE8NwortTTlZy4glzU60UhXWrr8nkjvBwH6aDFo7uFgfzoRrVAMDOBl8JsRftgCdUx3bSw5hhBk8LKgp6nCPcvXzU4xyKPXYQLQqW_fO0kw4nSNpJry59LcmoXnAvCs75t9yKJyNyqigHStyLFcGL1IIqnOT41l84p6px35y3GUo7cm-t9x8nCzk0QO8blmQJZVQww05Q9ZrHUwEYwKTpe3aUj6uFadWI_t_8TPU5QCOdJACbdWImcNXvWVflWvTMeeu1qgysS1eg1uQEFftEcOYxyw" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "content=Check out this design mockup for the new feature!" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" \
  | jq


# ───────────────────────────────────────────────────────────────────────────
# 4. SEND REPLY MESSAGE WITH IMAGE (multipart/form-data)
# ───────────────────────────────────────────────────────────────────────────

# Reply with both text AND an image attachment
# 🔐 Tests full flow: image upload, parent preview, unread increment for others
curl -X POST http://127.0.0.1:8005/api/messages/reply/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjE3MjgxLCJpYXQiOjE3Nzg2MTY5ODEsImp0aSI6IjY4OTM5YmI3M2Y5ZDRiYzRhNmU5MDM4ZTFjNDRkN2ViIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.lwXFh6HUefZHqRZYLIyxKgz9Mw-Kh8ABSUC_ODyXHGixTamq_v0x3nFnRsfb8-ldBfkiXnhhKPL6PlvgrxP18gNSpAO2Gb6Ll4k5-kAzMOy06HND_H5doiCT0aZyoavHo2HWyN72o6jJkBITOe9Zq7oFt3wEKWo1LHHuKzvKejuAQZ58QrsTLgGgGHQbT-W2WCjxyxVfgAwKsqFgyH1OL3jDcDe1j6vCMhJGcxEQ9L0pEkV-7UvvPIr8RJdVcJndoD3Hup852tFMNaGomEk07jl8aAh-wiFI4zChduuoe8Eii6Vn2iItYWlItaRjmJTU59bHIHNJFcViQY_UoIn19Q" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "content=Reply with image" \
  -F "parent_id=6d1a55bd-5dfd-425e-b392-737c7e2c2bb1" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" \ | jq
# Send a message with ONLY an image (empty content)
# Fixed: Send message with ONLY an image
curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjE3MDUwLCJpYXQiOjE3Nzg2MTY3NTAsImp0aSI6IjVjNTRiMGExMzkyZTRmOTFiOGE3ZjQ0NTIzZDYzYWE2IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.tIZW0NNb5h8cj2VEZKdROy669O_bJTiuDTa3exeUzu9W3TT0PGP8lOwmKcPFE8NwortTTlZy4glzU60UhXWrr8nkjvBwH6aDFo7uFgfzoRrVAMDOBl8JsRftgCdUx3bSw5hhBk8LKgp6nCPcvXzU4xyKPXYQLQqW_fO0kw4nSNpJry59LcmoXnAvCs75t9yKJyNyqigHStyLFcGL1IIqnOT41l84p6px35y3GUo7cm-t9x8nCzk0QO8blmQJZVQww05Q9ZrHUwEYwKTpe3aUj6uFadWI_t_8TPU5QCOdJACbdWImcNXvWVflWvTMeeu1qgysS1eg1uQEFftEcOYxyw" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "content=" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq


# ───────────────────────────────────────────────────────────────────────────
# 3. SEND REPLY MESSAGE - TEXT ONLY (application/json)
# ───────────────────────────────────────────────────────────────────────────

# Reply to an existing message with text only
# 🔐 Response includes parent_preview with image-over-text priority logic
# If parent has image: parent_preview shows image only; else shows content only
curl -X POST http://127.0.0.1:8005/api/messages/reply \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA5MjkzLCJpYXQiOjE3Nzg2MDg5OTMsImp0aSI6ImUzMTBjODdkMDk1NDRhZTI4YmRmMWRkODE2MzIwOGZmIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.tx5PUMdXL3WxYxPGxxD6CSvbl10GtjB6IxoXDd8mkieVytM5c0XsIeMPAvY-TjJxhTfWdbgDns3Xx70x3_s5VsKc9SGUtEGkvU-vI3xomgskthBbtg1NY9b81HR7qbF6mbUWORKndDYe7yhHkPXtVXonMoHeHj92uEiK1XtGTd_KHqn_nciRiviPu-fwTBs8M_KhbBJbmgu2rO7loGfMVIDaXhotjwSga4EWh2sIaR2u8Km3uXoRswU3A5Ni4ZmsoEtC2u7l3HqBGyHaZGAfYGyNgP30wTZcs3AhD1b2JoFAA4yv4d_xuSnAGEUWs0SINWhNg7pb_ZAEp2qviDC7Gw" \
  -d '{
    "room_id": "ca9ee906-a23a-4e07-9cb5-08684cf3b21a",
    "content": "I agree with this approach! Let me add some notes.",
    "parent_id": "1292b29f-f4c8-4ce2-9e77-42072d02129e"
  }' | jq

# Reply to a message that has an image (test image-over-text preview logic)
# parent_preview should show ONLY the image, not the text content
curl -X POST http://127.0.0.1:8005/api/messages/reply \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA5MjkzLCJpYXQiOjE3Nzg2MDg5OTMsImp0aSI6ImUzMTBjODdkMDk1NDRhZTI4YmRmMWRkODE2MzIwOGZmIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.tx5PUMdXL3WxYxPGxxD6CSvbl10GtjB6IxoXDd8mkieVytM5c0XsIeMPAvY-TjJxhTfWdbgDns3Xx70x3_s5VsKc9SGUtEGkvU-vI3xomgskthBbtg1NY9b81HR7qbF6mbUWORKndDYe7yhHkPXtVXonMoHeHj92uEiK1XtGTd_KHqn_nciRiviPu-fwTBs8M_KhbBJbmgu2rO7loGfMVIDaXhotjwSga4EWh2sIaR2u8Km3uXoRswU3A5Ni4ZmsoEtC2u7l3HqBGyHaZGAfYGyNgP30wTZcs3AhD1b2JoFAA4yv4d_xuSnAGEUWs0SINWhNg7pb_ZAEp2qviDC7Gw" \
  -d '{
    "room_id": "fbe31df8-6136-4ff6-bf6d-3b0f15164270",
    "content": "Love this screenshot! What tool did you use?",
    "parent_id": "660e8400-e29b-41d4-a716-446655440001"
  }' | jq


# =============================================================================
# MESSAGE UPDATE ACTIONS - CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace {message_id} with an actual message UUID from a previous send
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# 🔑 Authorization rules:
#    - DELETE/UPDATE content/image: Only the message SENDER can perform
#    - MARK received/seen: Only the message RECEIVER (NOT sender) can perform
# 
# Sample user IDs for testing:
#   "9e6c4138-3129-4875-8e72-25e4cb05905d"  # test9@example.com (receiver role)
#   "71885bbe-1f48-42b6-90e7-f988af5231dd"  # test@example.com (sender role)
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 5. DELETE MESSAGE (sender only)
# ───────────────────────────────────────────────────────────────────────────

# Soft-delete a message (only the sender can delete their own message)
# 🔐 Response includes is_deleted=true for UI sync
curl -X DELETE http://127.0.0.1:8005/api/messages/843e494f-114c-4b02-8f2c-2db7c7fc0805 \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyMDg0LCJpYXQiOjE3Nzg2MTE3ODQsImp0aSI6IjdkZWFlYjQzOWJiNDRlMDRhOWE4Y2I5ZTI1YjliY2VhIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.UEc5y47Xf6dgSMKfPv3_LQhMotnuRZ6GAG-nQ3FfxTPimstlNkWZAcDDlHsxt6_DRu9GfvtjdHmg2cBMyuyRab5BmXG58pNeU503zkLP6JysE9Iyjwgy71Q2bjCyPPCUayG9OhfaGDs5iVijSkesgNn4d__Hvze1rO7xTw28VdbPFQpvEd7yvxcni_P2DfzcsO-bFEwvJGq-dT6VWId41IhWBXMvyl1sIrs9y0bsvM6iZ0AiW3dflO4D6OwlbubPjQ8JIAi8IrvvHkjf1euKlPxfKD_m0HDhsB5NC_UvIq-JapH_UWp4_jI85O1B-F5GLRIFa1urQzUCsPm0UnyhHA" \
  | jq

# Expected response snippet:
# {
#   "id": "9eb7718e-e39e-4063-9071-64ea6803ede4",
#   "is_deleted": true,
#   "status": "SENT",
#   ...
# }

# ───────────────────────────────────────────────────────────────────────────
# 6. MARK MESSAGE AS RECEIVED (receiver only)
# ───────────────────────────────────────────────────────────────────────────

# Mark a message as RECEIVED (delivered to recipient's device)
# 🔐 ONLY the receiver (NOT the sender) can perform this action
# 🔐 Response includes status="RECEIVED"
curl -X PATCH http://127.0.0.1:8005/api/messages/eebc67b7-6866-482a-a523-0a232f9b7996/received \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyMTUwLCJpYXQiOjE3Nzg2MTE4NTAsImp0aSI6IjgxNGNlOGM4ZGJkYjRlZGE5M2I1OTcwZjk0MThiMmQ2IiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.aIDq2R115vEPuTtSAvk2QkioI5OhRRI6iOeKB_e0_gvSd04ukviM2br_3RPCG5f_xxs_3uc1MgSPtsSC-9ZbgdfWRrTjsaSHcgcDUw4Q4HXc83qoF9LlUoCB2OdlLyY-85uEZOcIAU7xJgrKAosicjZPqzCgdopEDwf9ZCEHUb-3DeP-4j_yI4hMsh5t0Wwhgvrb_acu_InBUiqmvWT3OcNZjQiZcWGG-D1CPFZAXamrWrNbkK1RlePE_vW3SFlLR3CJos7EEyH6a8BP3DIlol5ULPRpzqVmRDasZ-1xSfpGp6yyHQ96zsN1ZwxdN0z7LCH-2D8rpZxvaX-If6X5aw" \
  | jq

# Expected response snippet:
# {
#   "id": "9eb7718e-e39e-4063-9071-64ea6803ede4",
#   "status": "RECEIVED",
#   ...
# }

# ───────────────────────────────────────────────────────────────────────────
# 7. MARK MESSAGE AS SEEN (receiver only)
# ───────────────────────────────────────────────────────────────────────────

# Mark a message as SEEN (read by recipient)
# 🔐 ONLY the receiver (NOT the sender) can perform this action
# 🔐 Response includes status="SEEN" and seen_at timestamp
curl -X PATCH http://127.0.0.1:8005/api/messages/eebc67b7-6866-482a-a523-0a232f9b7996/seen \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyMTUwLCJpYXQiOjE3Nzg2MTE4NTAsImp0aSI6IjgxNGNlOGM4ZGJkYjRlZGE5M2I1OTcwZjk0MThiMmQ2IiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.aIDq2R115vEPuTtSAvk2QkioI5OhRRI6iOeKB_e0_gvSd04ukviM2br_3RPCG5f_xxs_3uc1MgSPtsSC-9ZbgdfWRrTjsaSHcgcDUw4Q4HXc83qoF9LlUoCB2OdlLyY-85uEZOcIAU7xJgrKAosicjZPqzCgdopEDwf9ZCEHUb-3DeP-4j_yI4hMsh5t0Wwhgvrb_acu_InBUiqmvWT3OcNZjQiZcWGG-D1CPFZAXamrWrNbkK1RlePE_vW3SFlLR3CJos7EEyH6a8BP3DIlol5ULPRpzqVmRDasZ-1xSfpGp6yyHQ96zsN1ZwxdN0z7LCH-2D8rpZxvaX-If6X5aw" \
  | jq

# Expected response snippet:
# {
#   "id": "9eb7718e-e39e-4063-9071-64ea6803ede4",
#   "status": "SEEN",
#   "created_at": "2024-01-15T10:30:00Z",  // Note: seen_at is stored internally
#   ...
# }

# ───────────────────────────────────────────────────────────────────────────
# 8. UPDATE MESSAGE CONTENT (sender only)
# ───────────────────────────────────────────────────────────────────────────

# Edit message text content (only the sender can edit their own message)
# 🔐 Request body: application/json with new_content field
# 🔐 Response includes updated content
curl -X PATCH http://127.0.0.1:8005/api/messages/eebc67b7-6866-482a-a523-0a232f9b7996/content \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyMTk5LCJpYXQiOjE3Nzg2MTE4OTksImp0aSI6ImZkYjk5OTk2NTIyMTRiOGY4MjEyYjAwYWUyZjg4ZTBlIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RlppKinqhBIjfRiSJx_KBgdoep4ljZNjQF8SVPgHju0m4x_LTodG4SyicmcjSccHcGHJhHSJsXL2TbmZZ9mtvzHy62NvtwSSqOiBgHg1-KvX8AE7akX9xokNmw2iUz7uoNlgcWKuJTwPQO3zFipTKqmqNLWWatIE88HGx4g9gHPa1suI_FQAhe9AbQnrIhaNp7-6hNCGXZ-hqLj21lEtmiKZIf9pf07iXSEoK7p2NYsgtN1Hmf-0a0SXTHJJEPsmDXqHkDhxBxdn1jwGR8LZkoEnSaCtEho3ulxHeSi4q7hoefQ5OFhPnEuhS30InAegjkCLALeFeyZHhIUlorRHgg" \
  -d '{
    "new_content": "Edited: Hello team! Starting the sprint planning session. [EDITED]"
  }' | jq

# Expected response snippet:
# {
#   "id": "9eb7718e-e39e-4063-9071-64ea6803ede4",
#   "content": "Edited: Hello team! Starting the sprint planning session. [EDITED]",
#   ...
# }

# ───────────────────────────────────────────────────────────────────────────
# 9. UPDATE MESSAGE IMAGE (sender only)
# ───────────────────────────────────────────────────────────────────────────

# Replace message image with a new upload (only the sender can update)
# 🔐 Request format: multipart/form-data with image file part
# 🔐 Response includes updated absolute image_url
curl -X PATCH http://127.0.0.1:8005/api/messages/eebc67b7-6866-482a-a523-0a232f9b7996/image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjE2MjE4LCJpYXQiOjE3Nzg2MTU5MTgsImp0aSI6IjhhNjVmYzU3ZDdjNTRlOGFiMGJiODAzNjExZGNiYThmIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.bz9DJUimHVdUVgsyI4-OfZbmcVE9eGkY9qj-nSJCkhQ0dBc3u6ZXed7eECsrhpI72W8eWy_SePrXykue4biUOehWd2KMe2nQQSS4WbNgUeDXEZEYsqPATNpS9UZFJ-UhVdmte4U0grlOwScexTeczGxd6KXK07L09Zy3bvy9ImMsJrpN3JYPm3UbcaB0eH1v0Z7p_Hq2-JE82ZKjfp9MVdA0u66szRwu3ZGWopQu565b44wVEEMCGIZqjT93G_TCuTXMrhGzeqjTgFReSXU-IZXdFJOmH7gK-ItUcd9Er0Dqn8m276sWrUisk81paYfExgFm0ayeHLkl54OZ_JgkAA" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" \
  | jq

# Expected response snippet:
# {
#   "id": "9eb7718e-e39e-4063-9071-64ea6803ede4",
#   "image_url": "http://127.0.0.1:8005/uploads/messages/abc123-new.jpg",
#   "has_image": true,
#   ...
# }

# Remove/clear the message image (only the sender can remove)
# 🔐 Use ?remove=true query param to explicitly clear the image
curl -X PATCH "http://127.0.0.1:8005/api/messages/eebc67b7-6866-482a-a523-0a232f9b7996/image?remove=true" \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyMTk5LCJpYXQiOjE3Nzg2MTE4OTksImp0aSI6ImZkYjk5OTk2NTIyMTRiOGY4MjEyYjAwYWUyZjg4ZTBlIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RlppKinqhBIjfRiSJx_KBgdoep4ljZNjQF8SVPgHju0m4x_LTodG4SyicmcjSccHcGHJhHSJsXL2TbmZZ9mtvzHy62NvtwSSqOiBgHg1-KvX8AE7akX9xokNmw2iUz7uoNlgcWKuJTwPQO3zFipTKqmqNLWWatIE88HGx4g9gHPa1suI_FQAhe9AbQnrIhaNp7-6hNCGXZ-hqLj21lEtmiKZIf9pf07iXSEoK7p2NYsgtN1Hmf-0a0SXTHJJEPsmDXqHkDhxBxdn1jwGR8LZkoEnSaCtEho3ulxHeSi4q7hoefQ5OFhPnEuhS30InAegjkCLALeFeyZHhIUlorRHgg" \
  | jq

# Expected response snippet:
# {
#   "id": "9eb7718e-e39e-4063-9071-64ea6803ede4",
#   "image_url": "",
#   "has_image": false,
#   ...
# }

# 






















# =============================================================================
# MESSAGE QUERY API - CURL TEST COMMANDS
# =============================================================================
# Base URL: http://127.0.0.1:8005
# Auth: JWT token via --cookie "access_token=..."
# All responses piped through jq for pretty JSON formatting
# 
# 🔑 Replace {room_id} with an actual room UUID from a previous room creation
# 🔑 Replace access_token values with fresh tokens from /zedvye_one/users/token/ endpoint
# 🔑 The requester_id is automatically extracted from UserContext (JWT token)
# 
# Sample user IDs for testing:
#   "9e6c4138-3129-4875-8e72-25e4cb05905d"  # test9@example.com
#   "71885bbe-1f48-42b6-90e7-f988af5231dd"  # test@example.com
#   "0beaf05f-3f45-466f-8913-9f218b0d7884"  # another test user
# 
# Expected response fields:
#   - is_mine: true when requester_id == message.sender_id
#   - sender_username: "You" when is_mine=true, else actual username
#   - parent_preview: present when message.is_reply=true (image-over-text priority)
#   - All image URLs are ABSOLUTE (converted from relative paths by MediaUrlService)
# =============================================================================

# ───────────────────────────────────────────────────────────────────────────
# 1. QUERY ALL ACTIVE MESSAGES BY ROOM (Basic Chat History)
# ───────────────────────────────────────────────────────────────────────────

# Fetch all active messages for a room as user test@example.com (sender role)
# 🔐 requester_id extracted from JWT → is_mine=true for messages sent by this user
# 🔐 sender_username will be "You" for own messages
# 🔐 Messages ordered by created_at ascending (oldest first) for chat history
curl -X GET http://127.0.0.1:8005/api/messages/room/ca9ee906-a23a-4e07-9cb5-08684cf3b21a \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjE2MDc1LCJpYXQiOjE3Nzg2MTU3NzUsImp0aSI6IjAzNmUxMzYwODYyNDQxYzlhY2M3ZjVhMTUyNzg4MDJkIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.Nlo2995JkFWYQkW_ywtzG48Q5XCVQlddZZ3an5nSGULAYJEzPRLcTe6T-SHSspYmfexBHhpiVXwovE_nq1MZjI1L_t1gYugZIrb2uByH3qeYdQAom58zzKAKK1520b-xOA4HvFNQpJlEp9L2kgypSDcg1ui6yqiQnXNNEC_XQJsKavVsq1cdFm6asDgIJ36gBpbPVq76FVBTPkburtc3udVrmTxFE0X9HS7wvQ7PVatR48xEfzaBY1UJ0_cnMpDfdxOaS3ezAq4lEopRbQjTuhZAOM9WVAr8TRZqx1rEYrqONBej26WlmiI-86yWkgXSIXJqbvINvNDu-XFwhf6UkA" \
  | jq

# Expected response structure (array of messages):
# [
#   {
#     "id": "550e8400-e29b-41d4-a716-446655440000",
#     "room_id": "ca9ee906-a23a-4e07-9cb5-08684cf3b21a",
#     "content": "Hello team! Starting the sprint planning session.",
#     "image_url": "",
#     "is_reply": false,
#     "parent_preview": null,
#     "created_at": "2024-01-15T10:30:00Z",
#     "is_mine": true,                    // ← true because requester is sender
#     "status": "SEEN",
#     "sender_username": "You",           // ← personalized as "You"
#     "sender_profile_image": "http://127.0.0.1:8005/uploads/users/profile/xyz.jpg",
#     "has_image": false,
#     "is_deleted": false,
#     "updated_at": "2024-01-15T10:30:00Z",
#     "seen_at": "2024-01-15T10:31:00Z"
#   },
#   {
#     "id": "660e8400-e29b-41d4-a716-446655440001",
#     "room_id": "ca9ee906-a23a-4e07-9cb5-08684cf3b21a",
#     "content": "Thanks for the update!",
#     "image_url": "http://127.0.0.1:8005/uploads/messages/abc123.jpg",  // ← absolute URL
#     "is_reply": false,
#     "parent_preview": null,
#     "created_at": "2024-01-15T10:32:00Z",
#     "is_mine": false,                   // ← false because different sender
#     "status": "RECEIVED",
#     "sender_username": "test9",         // ← actual username (not "You")
#     "sender_profile_image": "http://127.0.0.1:8005/uploads/users/profile/def456.jpg",
#     "has_image": true,
#     "is_deleted": false,
#     ...
#   }
# ]

# ───────────────────────────────────────────────────────────────────────────
# 2. QUERY MESSAGES AS DIFFERENT USER (Test is_mine & Personalization)
# ───────────────────────────────────────────────────────────────────────────

# Fetch same room as user test9@example.com (receiver role)
# 🔐 requester_id changed → is_mine flag flips for messages sent