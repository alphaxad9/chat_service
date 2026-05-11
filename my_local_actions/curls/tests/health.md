curl -s -X GET http://127.0.0.1:8005/api/chat/health  | jq
curl -s \
http://127.0.0.1:8005/api/users/012a181c-0c14-4aba-b868-4329555c3540 \
| jq

curl -s http://127.0.0.1:8005/api/v1/auth/test \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI2MjkyLCJpYXQiOjE3Nzg1MjU5OTIsImp0aSI6ImQ5ODcyMzllZGJjOTRmYzY5NWJhMmI1OWVjZDllYzUyIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.tE6pqNdUn3ZmY8dZGo99_04It2awhBvhsvvJHuSKNrulhUGQHUC_ZqBiAjRP9N5P_UIkjbtuMsfnWBtjcUrvRyc2i4eghJqOHGo0NpObjNCQVpbswZnJ9e63d9fvqyuQWJw_vcVQnL79wwsCpweuohW9r6Ig0RSIuUWSHS0H8CBBVz9tvWTfdQ5tEIWZzzI86zeXNWdTl4-nlxqUz6_JU0TJLmtoNnDl4ADXDprUWsd0cbY8OsHRHgJM28rqXwSXsIfct3Rt-LHebwDEFfzsXdCqissFwGT_mrcrrWsCf1mHZbjxVZhi75Ffq3jpCjU1fu3UckcxdSKD4GI6kqu8IQ" | jq


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



