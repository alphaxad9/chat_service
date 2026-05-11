<!-- chat_service/my_local_actions/curls/tests/posts/posts.md -->
curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test7@example.com",
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
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NDkyNzQzLCJpYXQiOjE3Nzg0OTI0NDMsImp0aSI6IjY3MWJhMDkxNTdjNjQ5OGI4Y2U4MDdjNjkyZjBhM2Y5IiwidXNlcl9pZCI6IjVkMDM3YzVmLTNhOGItNDZkYS04MGRmLTJmYTBlOTMyMGY4NCJ9.N2Df65eBN_--2mcncrC2h7jf58THuQ_QzNUpNGrR4TNeHNrU69tbr7ZN9GkiDrXZDSH1OeWw7T5GKlnJSAZtudf67eEmL8rFujJGR2GDdklx2zJH7cUcLv262ZVSr_wbU3xmaaLUIMNAlqWlOcJWTUU6g8N6e0AC84C0PM-wfnOg93OO97KRUkn7ZnK2ZZqIXD3Z-l7Nv8cjE8KRjtdpQm9xRwF71O8cp28Ru-baJjY6eAAlF-c8hwSM90I_3__os6NVFN7vgntlgGUM2X2fo2lLsIpi6-X0DPROFW8J8ugYuIj7kljof_J_mti1Ubbek18XhtCqy1CFuPknDDkf2g" \
  -F "content=My first image post with real upload! 🎉" \
  -F "image=@my_local_actions/curls/tests/posts/pexels-budget-bizar-92378004-18879101.jpg" \
  | jq