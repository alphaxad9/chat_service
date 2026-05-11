curl -s -X GET http://127.0.0.1:8005/api/chat/health  | jq
curl -s \
http://127.0.0.1:8005/api/users/012a181c-0c14-4aba-b868-4329555c3540 \
| jq

curl -s http://127.0.0.1:8005/api/v1/auth/test \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NDgyNDY3LCJpYXQiOjE3Nzg0ODIxNjgsImp0aSI6ImM0NTdiZGI4N2QzNjRiYjU5MjIyNjEzNTE2NjRkNmY5IiwidXNlcl9pZCI6IjVkMDM3YzVmLTNhOGItNDZkYS04MGRmLTJmYTBlOTMyMGY4NCJ9.XeP9SZn3h_V5--NRf94TacY3MRTAXr6fKI0S360PlKu4tcei5DSuYghJBHM_xSRa64cNa-uO88Oc7pua5vEFL82gtzmX_jsoQniDne1L93EZT0Fy1hlsrpaq5vDu_NdubCOp6wsrXXvLR74WT0ggFsmIYW8Zyd9-4Bw0bbEAIUG4ODhsD8YIVGbw8pcjjoAFHSLGoy0CJQWq4Jf8qdXkST4VCyl0fuU7gUW01UFbhGKHh0UUs0S7sqkklgYws0zsGPzE7_E7evtW0dgnQfxcAKcfhh_B1OljCE5WSWxwrFgEfPH1pcWk4fI6aJfiaP59WDHFvkrMsu46QoUxVIQvrA" | jq


curl -X POST http://127.0.0.1:8000/zedvye_one/users/register/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: A1egbCQUZi8wgNGFIjHeWzwMfAtwJXeNI2qvA4XoCoJlOwIlePFioVM6kF2HTkIK" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "email": "test7@example.com",
    "username": "testuser7",
    "password": "Test123!",
    "password2": "Test123!",
    "first_name": "John",
    "last_name": "Doe"
  }' | jq



curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test7@example.com",
    "password": "Test123!"
  }' | jq


curl -X GET http://127.0.0.1:8000/zedvye_one/users/users/012a181c-0c14-4aba-b868-4329555c3540/ \
  -H "Content-Type: application/json" \
  -H "X-Internal-Key: super-secret-internal-key-change-in-prod" \
  -c cookies.txt -b cookies.txt | jq



