curl -s -X GET http://127.0.0.1:8005/api/chat/health  | jq


curl -s http://127.0.0.1:8005/api/v1/auth/test \
  --cookie "access_token=YOUR_VALID_JWT" | jq


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
    "identifier": "test@example.com",
    "password": "Test123!"
  }' | jq
