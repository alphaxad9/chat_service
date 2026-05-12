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
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA5MjkzLCJpYXQiOjE3Nzg2MDg5OTMsImp0aSI6ImUzMTBjODdkMDk1NDRhZTI4YmRmMWRkODE2MzIwOGZmIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.tx5PUMdXL3WxYxPGxxD6CSvbl10GtjB6IxoXDd8mkieVytM5c0XsIeMPAvY-TjJxhTfWdbgDns3Xx70x3_s5VsKc9SGUtEGkvU-vI3xomgskthBbtg1NY9b81HR7qbF6mbUWORKndDYe7yhHkPXtVXonMoHeHj92uEiK1XtGTd_KHqn_nciRiviPu-fwTBs8M_KhbBJbmgu2rO7loGfMVIDaXhotjwSga4EWh2sIaR2u8Km3uXoRswU3A5Ni4ZmsoEtC2u7l3HqBGyHaZGAfYGyNgP30wTZcs3AhD1b2JoFAA4yv4d_xuSnAGEUWs0SINWhNg7pb_ZAEp2qviDC7Gw" \
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
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA4NjU5LCJpYXQiOjE3Nzg2MDgzNTksImp0aSI6IjQzYmVmOWFlMGRkZjQ5YjZhZGJlMmJjMmY1N2UzNTI0IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.AWfTeh2YIDsIOjWf4DANSZ1d8ITbPwEAvbTSdsxouudyXpVThTud_S-p0-E1XzxJ8vSUfL0q22U84xvwEcUOXa0MwEJGbRIpP16j4Q5ETULt8VEmPOkGf9RreKdRysgR07Yl2bXlAsYe6q2k5q6l04mOwNpp9RkoLB9FSavrLDrSQPv0-JJhcvMQpVrBq5yEn2YGfodDsSSb5cxx2COJpRw_XWdAhwXQpsecE3PV0bGr14-ggb85x5n6DZOO7YO9Hy4megcvAR-OT8wQKln8eTacs2hbPLGxjgdANAqaXMWus8A14ccgpvvJxxhk_ew5yU-BHTt-XxYfpkSDmykF9A" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "content=Check out this design mockup for the new feature!" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg" | jq


# Send a message with ONLY an image (empty content)
curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA5MjUxLCJpYXQiOjE3Nzg2MDg5NTEsImp0aSI6ImM4NTM4ZTRkZTFiNTRjZTc5NzU2Zjk0NWMxNGI4MmYwIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.jrly5tZrkY3BtS3jFo6mQbD3QOE48MJazNK8gulCjTjR1KoKZVtdoL7HSonNUlEyhy8ZAEvak5FNYebS0Fi09gKNQSZif6HJ7p-nwdllQvfVm__lZa7BSO655Wg1_SE0V4heZK5CgtOVvbjUp5SzfoARPNtpL3WXzmW6R3LLELNUZURRRyupJHRVQamrQf88Q9LdMOGAZZ2VSmod0j_4d-A5NekvYX4xTuvvE03Fj50ReNgrkDaK4zT2leBhqZ2Nc0MfDIyKIidYd0JlO14ajxbj7Af_H5dntchJ8oNSs2pT_-VBnnCSc7a93800RwPY56KFBpaIVyu4nCYFp7C8kg" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "room_id=fbe31df8-6136-4ff6-bf6d-3b0f15164270" \
  -F "content=" \
  -F "image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq

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

# ───────────────────────────────────────────────────────────────────────────
# 4. SEND REPLY MESSAGE WITH IMAGE (multipart/form-data)
# ───────────────────────────────────────────────────────────────────────────

# Reply with both text AND an image attachment
# 🔐 Tests full flow: image upload, parent preview, unread increment for others
curl -X POST http://127.0.0.1:8005/api/messages/reply/with-image \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg1NTMzLCJpYXQiOjE3Nzg1ODUyMzMsImp0aSI6IjNjZDZlMzc1Yzg3NTQ5MTdiNDExMjY2NjZhNzMwZDRkIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.ALrFM5Zo1lxN9kB4WstPR5hV9iIKna_wh0-hKJxVH2JF4FnIQZz5D-CclPSu_xgAl2mfYQ_OUrQU1CFr8hMaWUWd3VX4ImtJtXxY1fkUufnAELqViTTUrl7X8EOc1jZtzpEbncZNdJS9kvc4wJqu5QhZV_eRmQFY7CGsDacAKsbJfqGAk7ZIpSPUmJpfo0DiMif_ei9kXoELaEXcRE_P6U1rOxgSXkWRmL0MkGDiBlwtQy4MzCGU8pZB7qImYU9UKZWkfbZOi-VnkOUN_Sc95sh2foXbZV4ep-jV6fEIVegn5SdDYUXfTJ7IU7lPhZCjeXf9SYyaI9SW8gfHsnqLYw" \
  -F "room_id=fbe31df8-6136-4ff6-bf6d-3b0f15164270" \
  -F "content=Here is my revised version based on your feedback" \
  -F "parent_id=550e8400-e29b-41d4-a716-446655440000" \
  -F "image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" | jq
