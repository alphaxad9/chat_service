(ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 
curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test7@example.com",
    "password": "Test123!"
  }' | jq

  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  1461  100  1390  100    71    568     29  0:00:02  0:00:02 --:--:--   597
{
  "message": "testuser7 logged in successfully",
  "user": {
    "id": "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
    "username": "testuser7",
    "email": "test7@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_picture": null
  },
  "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NDg5NTEzLCJpYXQiOjE3Nzg0ODkyMTMsImp0aSI6IjA2MDI1ZjM0OWQyYjQzNjhhNzM4MzNkYjA2MWQ4ZDQ1IiwidXNlcl9pZCI6IjVkMDM3YzVmLTNhOGItNDZkYS04MGRmLTJmYTBlOTMyMGY4NCJ9.ZMUAXrI3j22QZlrjWguLdPjVklEWJgEB99OQ_VZ-A72L1DKjX95rxyDBRgGZFdc-jU_V5ngjYbVVifm-jCcPRJzLFfsyqOeqUPSe_VzRA1r3Cw0xvmR7bhKKg6NqqBzJH9T8tKsisqgVK17Hh2SVkHAa2zkrjjAHXsDL8FoIV3z7F_ku0nvNX8sq5BEB0xzvr7-1hKepUdrkabtip6Dw_djxJuraNR-Y9-uNHTbczYkKp68FyZfutU-FkW--VYMH7_UXnk6_-Mlw6_5Bm8utCUcli_BKUj2Pzy4KkncET6T4Ryjw8PFDCRu_RZRmSWCK1Rwz6umXXoHcLwLlzSJeXA",
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTc3ODU3NTYxMywiaWF0IjoxNzc4NDg5MjEzLCJqdGkiOiIyNzA4MzdjYzc0ZjM0ZmIyYjg1M2RhNGFjZjExNWZmMiIsInVzZXJfaWQiOiI1ZDAzN2M1Zi0zYThiLTQ2ZGEtODBkZi0yZmEwZTkzMjBmODQifQ.pT7-CtJU6lXg4hwwkiYts22JdGO96vzBKR-LkF6pnn4dYcOMR2MD6D1xxDwJ3A5decVNf3JnV4vYK7_OkOhtOmHGiMsNGuSL-o9bz4lsz6j5bfzj6ZgcWyATk6fTl5ktkAcjNgPi-d9BGfOpUeDjJtPCpWrg7fnfJjf3sQctwCivisq-qgA2maNxoySlnIh-laGuqJ6L8XQsWxDFI80wpp1B_a-MdKuoa8ysBlF3YtSUwFKWONUlu6zxk4IEZ9cDhNV3_7R5Ncut5OY_AmIZOXMZa3i0aWut0tMIZYxdIV91WazzWMEQshvXTFQBbJVykXFONSr4bpRFj7Gh_c5Xxg"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8005/api/posts \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NDg5NTEzLCJpYXQiOjE3Nzg0ODkyMTMsImp0aSI6IjA2MDI1ZjM0OWQyYjQzNjhhNzM4MzNkYjA2MWQ4ZDQ1IiwidXNlcl9pZCI6IjVkMDM3YzVmLTNhOGItNDZkYS04MGRmLTJmYTBlOTMyMGY4NCJ9.ZMUAXrI3j22QZlrjWguLdPjVklEWJgEB99OQ_VZ-A72L1DKjX95rxyDBRgGZFdc-jU_V5ngjYbVVifm-jCcPRJzLFfsyqOeqUPSe_VzRA1r3Cw0xvmR7bhKKg6NqqBzJH9T8tKsisqgVK17Hh2SVkHAa2zkrjjAHXsDL8FoIV3z7F_ku0nvNX8sq5BEB0xzvr7-1hKepUdrkabtip6Dw_djxJuraNR-Y9-uNHTbczYkKp68FyZfutU-FkW--VYMH7_UXnk6_-Mlw6_5Bm8utCUcli_BKUj2Pzy4KkncET6T4Ryjw8PFDCRu_RZRmSWCK1Rwz6umXXoHcLwLlzSJeXA" \
  -d '{
    "content": "My first Spring Boot post",
    "image_url": null
  }' | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   492    0   421  100    71    615    103 --:--:-- --:--:-- --:--:--   720
{
  "post_id": "a9bdc2c7-adb4-4597-9b27-d9eba8f158d4",
  "author": {
    "user_id": "5d037c5f-3a8b-46da-80df-2fa0e9320f84",
    "username": "testuser7",
    "email": "test7@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_picture": null
  },
  "content": "My first Spring Boot post",
  "image_url": null,
  "created_at": "2026-05-11T10:47:07.915744799",
  "updated_at": "2026-05-11T10:47:07.915744799",
  "is_deleted": false,
  "has_image": false,
  "is_active": true
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 
)