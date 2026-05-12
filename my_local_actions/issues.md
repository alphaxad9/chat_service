see why does this one works (ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X PATCH http://127.0.0.1:8005/api/messages/eebc67b7-6866-482a-a523-0a232f9b7996/image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyMTk5LCJpYXQiOjE3Nzg2MTE4OTksImp0aSI6ImZkYjk5OTk2NTIyMTRiOGY4MjEyYjAwYWUyZjg4ZTBlIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.RlppKinqhBIjfRiSJx_KBgdoep4ljZNjQF8SVPgHju0m4x_LTodG4SyicmcjSccHcGHJhHSJsXL2TbmZZ9mtvzHy62NvtwSSqOiBgHg1-KvX8AE7akX9xokNmw2iUz7uoNlgcWKuJTwPQO3zFipTKqmqNLWWatIE88HGx4g9gHPa1suI_FQAhe9AbQnrIhaNp7-6hNCGXZ-hqLj21lEtmiKZIf9pf07iXSEoK7p2NYsgtN1Hmf-0a0SXTHJJEPsmDXqHkDhxBxdn1jwGR8LZkoEnSaCtEho3ulxHeSi4q7hoefQ5OFhPnEuhS30InAegjkCLALeFeyZHhIUlorRHgg" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" \
  | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 2278k    0   578  100 2278k   6470  24.9M --:--:-- --:--:-- --:--:-- 24.7M
{
  "id": "eebc67b7-6866-482a-a523-0a232f9b7996",
  "room_id": "ca9ee906-a23a-4e07-9cb5-08684cf3b21a",
  "content": "Edited: Hello team! Starting the sprint planning session. [EDITED]",
  "image_url": "http://127.0.0.1:8005/uploads/messages/d4c3ad25-9584-4548-a48b-c67542ea7aa5.jpg",
  "is_reply": false,
  "parent_preview": null,
  "created_at": "2026-05-12T20:50:29.983961",
  "is_mine": true,
  "status": "SEEN",
  "sender_username": "testuser9",
  "sender_profile_image": "http://127.0.0.1:8005http://127.0.0.1:8000/media/profile_pictures/pexels-budget-bizar-92378004-18879101.jpg",
  "has_image": true,
  "is_deleted": false
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 


) but these two wont (# Reply with both text AND an image attachment
# 🔐 Tests full flow: image upload, parent preview, unread increment for others
curl -X POST http://127.0.0.1:8005/api/messages/reply/with-image \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTg1NTMzLCJpYXQiOjE3Nzg1ODUyMzMsImp0aSI6IjNjZDZlMzc1Yzg3NTQ5MTdiNDExMjY2NjZhNzMwZDRkIiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.ALrFM5Zo1lxN9kB4WstPR5hV9iIKna_wh0-hKJxVH2JF4FnIQZz5D-CclPSu_xgAl2mfYQ_OUrQU1CFr8hMaWUWd3VX4ImtJtXxY1fkUufnAELqViTTUrl7X8EOc1jZtzpEbncZNdJS9kvc4wJqu5QhZV_eRmQFY7CGsDacAKsbJfqGAk7ZIpSPUmJpfo0DiMif_ei9kXoELaEXcRE_P6U1rOxgSXkWRmL0MkGDiBlwtQy4MzCGU8pZB7qImYU9UKZWkfbZOi-VnkOUN_Sc95sh2foXbZV4ep-jV6fEIVegn5SdDYUXfTJ7IU7lPhZCjeXf9SYyaI9SW8gfHsnqLYw" \
  -F "room_id=fbe31df8-6136-4ff6-bf6d-3b0f15164270" \
  -F "content=Here is my revised version based on your feedback" \
  -F "parent_id=550e8400-e29b-41d4-a716-446655440000" \
  -F "image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg;type=image/jpeg" | jq
)(# Send a message with ONLY an image (empty content)
curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA5MjUxLCJpYXQiOjE3Nzg2MDg5NTEsImp0aSI6ImM4NTM4ZTRkZTFiNTRjZTc5NzU2Zjk0NWMxNGI4MmYwIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.jrly5tZrkY3BtS3jFo6mQbD3QOE48MJazNK8gulCjTjR1KoKZVtdoL7HSonNUlEyhy8ZAEvak5FNYebS0Fi09gKNQSZif6HJ7p-nwdllQvfVm__lZa7BSO655Wg1_SE0V4heZK5CgtOVvbjUp5SzfoARPNtpL3WXzmW6R3LLELNUZURRRyupJHRVQamrQf88Q9LdMOGAZZ2VSmod0j_4d-A5NekvYX4xTuvvE03Fj50ReNgrkDaK4zT2leBhqZ2Nc0MfDIyKIidYd0JlO14ajxbj7Af_H5dntchJ8oNSs2pT_-VBnnCSc7a93800RwPY56KFBpaIVyu4nCYFp7C8kg" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "room_id=fbe31df8-6136-4ff6-bf6d-3b0f15164270" \
  -F "content=" \
  -F "image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq
) i still get (ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjEyNDIwLCJpYXQiOjE3Nzg2MTIxMjAsImp0aSI6ImViODdmNWI4MmVhMTQzZjk5OGIyZjkwYzA1OGU0Njg3IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.J_jC2dK1PXfOVs1j8WRZMHykR4iuXa1hEzxRg9TVeB-N7iavVPhz0YdrbmP7mDiBGwQhhXAm7HYZ9zFIis_SvKbM5PraB9_E4PFzk5wTNffNyTPJzRd4TA_1W7hWE1fCamUMGdP2opwdlMCTi01Xi1ZtiDHVCBd_jS0R6ih_4nNEGUqw8kd7dNGgivjyI2Ant5DQXbIWSAFfGTjej25tGKBSMWdck3BSO6QtnV7rir82jRB1Wgulvjq_IRP-qq7MUMsvfTLcffGUBGq9zIvExB8wN5e42H3w0ehQ6An8qzDeFFMFleTEEctreJVtFKcGIWeWUtbp31qVKuPqckBf_Q" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "room_id=fbe31df8-6136-4ff6-bf6d-3b0f15164270" \
  -F "content=" \
  -F "image=@$HOME/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-myburgh-4816921.jpg;type=image/jpeg" | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 2278k    0   120  100 2278k   3283  60.8M --:--:-- --:--:-- --:--:-- 61.8M
{
  "timestamp": "2026-05-12T18:55:30.867Z",
  "status": 415,
  "error": "Unsupported Media Type",
  "path": "/api/messages/with-image"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 
) is it the curl issue?