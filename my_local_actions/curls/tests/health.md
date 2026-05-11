curl -s -X GET http://127.0.0.1:8005/api/chat/health  | jq
curl -s \
http://127.0.0.1:8005/api/users/012a181c-0c14-4aba-b868-4329555c3540 \
| jq

curl -s http://127.0.0.1:8005/api/v1/auth/test \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMxNTA4LCJpYXQiOjE3Nzg1MzEyMDgsImp0aSI6ImE4YTUwMmZjZDYzYzRlMDliMDFhZmEyYTA4ZjgxMjUzIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.ZqGzOVu7Xzsk5_SPqJPSA4CKeVAKveYM-c648SSuDLKtxaCGfHcI3Z-bb44soEmsI2pw87vRHK1qA0iF32MKxft7Et2Pd2OgA4ULjXOQ-Yu4jlzdYKDhGbuAQT_9Y8kJIM6DjZzn7F_bPH8BTJP1auFXkVQhr1R7rXibC_xL6HOrIrJxWcR-F-ra8vRso2ad971OreS5-4uDN8lBcFCpkb1tUAlMxWnJ_0w2IhIc8hEeUY5FUHvqd71DM782voMgURJIeHU2nABRTQXOJ7o49Ol4hIUkQ7765jM7cx65CH1qkN-KDwBznFloiGLYzE0thRytJRHTaSkyVg-jhDG-QA" | jq


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



