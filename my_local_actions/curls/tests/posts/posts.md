<!-- chat_service/my_local_actions/curls/tests/posts/posts.md -->
curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test9@example.com",
    "password": "Test123!"
  }' | jq




curl -X POST http://127.0.0.1:8005/api/posts \
  -H "Content-Type: application/json" \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NDg5NTEzLCJpYXQiOjE3Nzg0ODkyMTMsImp0aSI6IjA2MDI1ZjM0OWQyYjQzNjhhNzM4MzNkYjA2MWQ4ZDQ1IiwidXNlcl9pZCI6IjVkMDM3YzVmLTNhOGItNDZkYS04MGRmLTJmYTBlOTMyMGY4NCJ9.ZMUAXrI3j22QZlrjWguLdPjVklEWJgEB99OQ_VZ-A72L1DKjX95rxyDBRgGZFdc-jU_V5ngjYbVVifm-jCcPRJzLFfsyqOeqUPSe_VzRA1r3Cw0xvmR7bhKKg6NqqBzJH9T8tKsisqgVK17Hh2SVkHAa2zkrjjAHXsDL8FoIV3z7F_ku0nvNX8sq5BEB0xzvr7-1hKepUdrkabtip6Dw_djxJuraNR-Y9-uNHTbczYkKp68FyZfutU-FkW--VYMH7_UXnk6_-Mlw6_5Bm8utCUcli_BKUj2Pzy4KkncET6T4Ryjw8PFDCRu_RZRmSWCK1Rwz6umXXoHcLwLlzSJeXA" \
  -d '{
    "content": "My first Spring Boot post",
    "image_url": null
  }' | jq

# Use an image you have locally, e.g., the one in your project:
curl -X POST http://127.0.0.1:8005/api/posts \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NDk3MDA5LCJpYXQiOjE3Nzg0OTY3MDksImp0aSI6IjM5Mjc3ZDZhZGQ2ZjRhNGFhNzdiOTMxNjZiN2RjNTM0IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.AOIWsrykzohn2kDfuRgyBI1ESO65pssPft11cbNNl231l2BQKvqaNTZZVBuX4J5FyeXeHi5-isJJyFh2sarLbnNTjev2EuLnBatW3berub_S-qshfVzUgIBElLzQlN44PTInVUvIzTl7qeGyeQwzlyv6YaWpfX1Tsg_VuoHQfl_HxMI2DVPiBtLRznxotpIhWB-rR-DSt8ER5H1L7gIwGyzUkWXSxpIzdOcoZENq-G3urPSBuAQRAvcUoe1ox859rPnJBxE_TFapnYhhSmCCeQYN5YxfTyrB1bTrI5hS01K4eLirhTsWC5qmEJoo2wse9CG2mh_xDphcMk43d5YX0A" \
  -F "content=My first image post with real upload! 🎉" \
  -F "image=@my_local_actions/curls/tests/posts/pexels-budget-bizar-92378004-18879101.jpg" \
  | jq