see (ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "test9@example.com",
    "password": "Test123!"
  }' | jq

  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  1524  100  1453  100    71    666     32  0:00:02  0:00:02 --:--:--   699
{
  "message": "testuser9 logged in successfully",
  "user": {
    "id": "71885bbe-1f48-42b6-90e7-f988af5231dd",
    "username": "testuser9",
    "email": "test9@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_picture": "/media/profile_pictures/pexels-budget-bizar-92378004-18879101.jpg"
  },
  "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMyMjEzLCJpYXQiOjE3Nzg1MzE5MTMsImp0aSI6ImU4ZDNhMjRiZGQyYTQ3MDBiNzA2MmYyYTBmYjE3NjYyIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.VzLnqLMBYHN9k2kLHkFhcMr1fGSMZl7C76ZYpRaayMPrEKq94MTbn6q2ooz3bJiAct-OUzrSxVmn4vXoytzBQCKxVd-EhQqbRGRznmV7S65eyLHPSmMxdpVXpLmzENUQLDnw0F0Rv1Y7HPC1RbwU_b1yaU7gMVt-aGccCLcxJVZ6uOBj1l5drhMeNCeiDB34YnN7lS4cFOLD4JYzXb2lg5q1WVaEDWJOUawpNiY-8GyL09JYfXhn75HME6OJH4guYWaqPcIBAMLi3LBie8JeijKj_xim5Gl7I9Y1XN52RPvh_lZV6P0l9kJ6z66ZCQBCcywZ0n2RxVQlUL74x9oT1A",
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTc3ODYxODMxMywiaWF0IjoxNzc4NTMxOTEzLCJqdGkiOiI3NjI3MzBlOTFlNDI0OGZhYWE1OTc3MDBlMjkyYTg0YiIsInVzZXJfaWQiOiI3MTg4NWJiZS0xZjQ4LTQyYjYtOTBlNy1mOTg4YWY1MjMxZGQifQ.WGYg0atqithPyx6_40qrZNiTZ88_9BhqVkraFO32_WEUUxJi7qIJ-URalbn6hnRIK1xvgr8xwehc-_KtTClNOmWyv78vnQ0F8ZJipvqmphPrKDuGfTUHI5M-5JlwlEdHUC5uC8sqWXBYA9wssJFUSEd9cbSvNENQ_UxTbG9E8sintwhKkKNZSbtlorOP6I6aONBWuCZLdVL5vW8vBNbPbVzfnOI8SyTUDJLQKcC17zkeYQEY2VOsEP5cVQ1LHvbUY-NwH6JgZNR5v0PTKhtyd1-Td4ZNlVD_Q3InL-jfUkZbavb716pCGPjBQRRNxDYg5tQFKXaL9i43AzgGvdKBxQ"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8005/api/rooms/0beaf05f-3f45-466f-8913-9f218b0d7884/members \
  -H "Content-Type: application/json" \
  --cookie "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTI1MDI2LCJpYXQiOjE3Nzg1MjQ3MjYsImp0aSI6IjA2YzcyMzgyZmU2YzQ5NDliOWYzYWI1MTU4MTdhM2RiIiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.rqdHagJIlyf4v_ccLy5SIon_fRylQCHrgHWEs6_pCpr9nlodvuNwNTLZhzVcHCNlZ-SPU2SvZ8dsP44bwVEtzPJiPVfcQQSjWs3t13wnJq0ztXl2eQMRED7ueYaoMMjgxuuexCC_RHzsUDx6s355TzPGHV41O9plL9ktPFNpaAzQeFBIxMAlqjnZKNKpgM25LDxOGADllH4VI1vCmBc7DY1wv1sFqfd_ecfwLo4vKKWTwkDoxh6kEZSBXoBT2Ch0S9Mn5llFckzmJCbOsFJbWx8GWc1AMZqrrp0fYOtSbmOhpx0A8RUGzlGlJeZPrMLDqXbLe6a4tm1UPooxeXuvUQ" \
  -d '{
    "user_id": "9e6c4138-3129-4875-8e72-25e4cb05905d",
    "status": "USER"
  }' | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   383    0   302  100    81   1141    306 --:--:-- --:--:-- --:--:--  1450
{
  "member_id": "a33a1703-2fec-4502-8189-60aadf1961fc",
  "user": {
    "user_id": "9e6c4138-3129-4875-8e72-25e4cb05905d",
    "username": "testuser",
    "email": "test@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_picture": null
  },
  "room_id": "0beaf05f-3f45-466f-8913-9f218b0d7884",
  "status": "USER",
  "is_admin": false
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/ \
  -H "Content-Type: application/json" \
  -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "identifier": "testuser",
    "password": "Test123!"
  }' | jq

  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  1449  100  1387  100    62    705     31  0:00:02  0:00:01  0:00:01   736
{
  "message": "testuser logged in successfully",
  "user": {
    "id": "9e6c4138-3129-4875-8e72-25e4cb05905d",
    "username": "testuser",
    "email": "test@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_picture": null
  },
  "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMyMjYwLCJpYXQiOjE3Nzg1MzE5NjAsImp0aSI6IjcxZTVjM2VhZmVjYTQ4NjZiODdjYTdiNjE5ZWI5Njk0IiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.pVK2sMsb3BFQgDg6vcZz0eqeZGIrkxMhXsr3ioi1ttABa-rwTdGSC98YENvVgxtoJAmEn5gGTervrrNRDd0bjXJmAv4Qvh67T0h0kwk2kJBNfp_aV3jxZeTcOGFAHhPlUA28dvzsgfoMK55jepH9H953ih9ZN2DopxyDFWCpBubmTQOXrPtYjFWmhncxVnlUqVfmVj38wLQ7xEZJtUR2Uwr9TY7Mma7AruQlr-VkDQb8kh1wDsE1K1q7Kdy5nu2kn6Zk49vXG2aGvYMx-wLrOWwPzd2ACMtA6wT94heV9EZuUik6R03zPlU57pPHq34kqVFryKsg8t22lHleSYgACQ",
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTc3ODYxODM2MCwiaWF0IjoxNzc4NTMxOTYwLCJqdGkiOiJmYzE0NjhmNTNlZTc0NjQ1ODIxMzRkMDM4MmM5Nzc4ZCIsInVzZXJfaWQiOiI5ZTZjNDEzOC0zMTI5LTQ4NzUtOGU3Mi0yNWU0Y2IwNTkwNWQifQ.MNJHEq_nFnvlSI-FYY0kyGqmJYTV0ZAhxqVuwfg3CIYFLcxzm5kwxsKNvWP0E_lbfVnDfmw3oGAQ3_NuCA-okhXwLYy2fBCO1ReNXf8iV7Q7StqtVqry85KDDnKY1mECazjpY_4Clyh8zHONH8fMt3jsqppmJZF87o8IPTBWn6WheNq6Si8IjuqEGSk1SgHFmTPIYmoVyY6nTZbzD-f5dJp0NKaznkEPFzChgHAAvyx_u1wCRFgEWVFd5-S2uLb3Qt3NpfBpHOKgk2bhRKUA5C6dO9Uvd0g02cUWu45CJUpeOPDXrJXO3Al_D2yoHvC9rCA5LR3GQctu2pikm7wjlg"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 
curl -X DELETE http://127.0.0.1:8005/api/members/0beaf05f-3f45-466f-8913-9f218b0d7884/leave \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMyMjYwLCJpYXQiOjE3Nzg1MzE5NjAsImp0aSI6IjcxZTVjM2VhZmVjYTQ4NjZiODdjYTdiNjE5ZWI5Njk0IiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.pVK2sMsb3BFQgDg6vcZz0eqeZGIrkxMhXsr3ioi1ttABa-rwTdGSC98YENvVgxtoJAmEn5gGTervrrNRDd0bjXJmAv4Qvh67T0h0kwk2kJBNfp_aV3jxZeTcOGFAHhPlUA28dvzsgfoMK55jepH9H953ih9ZN2DopxyDFWCpBubmTQOXrPtYjFWmhncxVnlUqVfmVj38wLQ7xEZJtUR2Uwr9TY7Mma7AruQlr-VkDQb8kh1wDsE1K1q7Kdy5nu2kn6Zk49vXG2aGvYMx-wLrOWwPzd2ACMtA6wT94heV9EZuUik6R03zPlU57pPHq34kqVFryKsg8t22lHleSYgACQ" | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    33    0    33    0     0     84      0 --:--:-- --:--:-- --:--:--    83
{
  "error": "Authentication failed"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 
curl -X PATCH http://127.0.0.1:8005/api/members/98787ef6-f118-400c-ad64-66e5634e664c/read \
  --cookie "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NTMyMjYwLCJpYXQiOjE3Nzg1MzE5NjAsImp0aSI6IjcxZTVjM2VhZmVjYTQ4NjZiODdjYTdiNjE5ZWI5Njk0IiwidXNlcl9pZCI6IjllNmM0MTM4LTMxMjktNDg3NS04ZTcyLTI1ZTRjYjA1OTA1ZCJ9.pVK2sMsb3BFQgDg6vcZz0eqeZGIrkxMhXsr3ioi1ttABa-rwTdGSC98YENvVgxtoJAmEn5gGTervrrNRDd0bjXJmAv4Qvh67T0h0kwk2kJBNfp_aV3jxZeTcOGFAHhPlUA28dvzsgfoMK55jepH9H953ih9ZN2DopxyDFWCpBubmTQOXrPtYjFWmhncxVnlUqVfmVj38wLQ7xEZJtUR2Uwr9TY7Mma7AruQlr-VkDQb8kh1wDsE1K1q7Kdy5nu2kn6Zk49vXG2aGvYMx-wLrOWwPzd2ACMtA6wT94heV9EZuUik6R03zPlU57pPHq34kqVFryKsg8t22lHleSYgACQ" | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    33    0    33    0     0   1276      0 --:--:-- --:--:-- --:--:--  1320
{
  "error": "Authentication failed"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ 

) even a newly created member ( nts: []

2026-05-11T22:40:26.458+02:00  INFO 370553 --- [chat_service] [           main] c.e.chat_service.ChatServiceApplication  : Starting ChatServiceApplication using Java 21.0.10 with PID 370553 (/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/target/classes started by ishimwe in /home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service)
2026-05-11T22:40:26.462+02:00 DEBUG 370553 --- [chat_service] [           main] c.e.chat_service.ChatServiceApplication  : Running with Spring Boot v4.0.6, Spring v7.0.7
2026-05-11T22:40:26.464+02:00  INFO 370553 --- [chat_service] [           main] c.e.chat_service.ChatServiceApplication  : No active profile set, falling back to 1 default profile: "default"
2026-05-11T22:40:27.133+02:00  INFO 370553 --- [chat_service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-05-11T22:40:27.211+02:00  INFO 370553 --- [chat_service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repositoutil.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:946) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:480) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:57) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at java.base/java.lang.Thread.run(Thread.java:1583) ~[na:na]
Caused by: com.example.chat_service.domain.members.exceptions.MemberNotFoundError: Member not found
	at com.example.chat_service.infrastructure.persistence.members.repositories.MemberCommandOrmRepository.lambda$load$0(MemberCommandOrmRepository.java:118) ~[classes/:na]
	at java.base/java.util.Optional.orElseThrow(Optional.java:403) ~[na:na]
	at com.example.chat_service.infrastructure.persistence.members.repositories.MemberCommandOrmRepository.load(MemberCommandOrmRepository.java:118) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:190) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:158) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:135) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor$1.proceedWithInvocation(TransactionInterceptor.java:133) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:371) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:130) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:719) ~[spring-aop-7.0.7.jar:7.0.7]
	at com.example.chat_service.infrastructure.persistence.members.repositories.MemberCommandOrmRepository$$SpringCGLIB$$0.load(<generated>) ~[classes/:na]
	at com.example.chat_service.application.members.services.impl.MemberCommandServiceImpl.leaveRoom(MemberCommandServiceImpl.java:117) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:190) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:158) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor$1.proceedWithInvocation(TransactionInterceptor.java:133) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:371) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:130) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:719) ~[spring-aop-7.0.7.jar:7.0.7]
	at com.example.chat_service.application.members.services.impl.MemberCommandServiceImpl$$SpringCGLIB$$0.leaveRoom(<generated>) ~[classes/:na]
	at com.example.chat_service.application.members.handlers.MemberCommandHandler.leaveRoom(MemberCommandHandler.java:179) ~[classes/:na]
	at com.example.chat_service.api.chat.MemberController.leaveRoom(MemberController.java:151) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:252) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:184) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:934) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:853) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:86) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:963) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:866) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1000) ~[spring-webmvc-7.0.7.jar:7.0.7]
	... 35 common frames omitted

2026-05-11T22:41:06.715+02:00 DEBUG 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.i.s.JWTAuthenticationFilter        : Extracted token from cookie 'access_token'
2026-05-11T22:41:06.715+02:00 DEBUG 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.i.s.JWTAuthenticationFilter        : Verifying token for request: /api/members/98787ef6-f118-400c-ad64-66e5634e664c/read
2026-05-11T22:41:06.718+02:00 DEBUG 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.i.s.JWTAuthenticationFilter        : Authenticated user_id=9e6c4138-3129-4875-8e72-25e4cb05905d
2026-05-11T22:41:06.720+02:00 DEBUG 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.api.chat.MemberController          : Processing markAllRead: member_id=98787ef6-f118-400c-ad64-66e5634e664c, requester_id=9e6c4138-3129-4875-8e72-25e4cb05905d
2026-05-11T22:41:06.721+02:00  INFO 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.a.m.handlers.MemberCommandHandler  : Marking all messages read: member_id=98787ef6-f118-400c-ad64-66e5634e664c, requester_id=9e6c4138-3129-4875-8e72-25e4cb05905d
2026-05-11T22:41:06.725+02:00 DEBUG 370553 --- [chat_service] [nio-8005-exec-2] org.hibernate.SQL                        : select me1_0.id,me1_0.is_left,me1_0.joined_at,me1_0.room_id,me1_0.status,me1_0.unread_messages,me1_0.updated_at,me1_0.user_id from members me1_0 where me1_0.id=? and (me1_0.is_left = false)
Hibernate: select me1_0.id,me1_0.is_left,me1_0.joined_at,me1_0.room_id,me1_0.status,me1_0.unread_messages,me1_0.updated_at,me1_0.user_id from members me1_0 where me1_0.id=? and (me1_0.is_left = false)
2026-05-11T22:41:06.731+02:00  WARN 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.a.m.s.i.MemberCommandServiceImpl   : Member not found for markAllRead operation: member_id=98787ef6-f118-400c-ad64-66e5634e664c
2026-05-11T22:41:06.733+02:00 ERROR 370553 --- [chat_service] [nio-8005-exec-2] c.e.c.i.s.JWTAuthenticationFilter        : Unexpected error during JWT verification for request /api/members/98787ef6-f118-400c-ad64-66e5634e664c/read

jakarta.servlet.ServletException: Request processing failed: com.example.chat_service.domain.members.exceptions.MemberNotFoundError: Member not found
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1008) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:877) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:710) ~[tomcat-embed-core-11.0.21.jar:6.1]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:128) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-11.0.21.jar:11.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:107) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at com.example.chat_service.infrastructure.security.JWTAuthenticationFilter.doFilterInternal(JWTAuthenticationFilter.java:77) ~[classes/:na]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-7.0.7.jar:7.0.7]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:107) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-7.0.7.jar:7.0.7]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:107) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-7.0.7.jar:7.0.7]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:107) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:199) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-7.0.7.jar:7.0.7]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:107) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:165) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:77) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:492) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:113) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:83) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:72) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:341) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:397) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:903) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1801) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:946) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:480) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:57) ~[tomcat-embed-core-11.0.21.jar:11.0.21]
	at java.base/java.lang.Thread.run(Thread.java:1583) ~[na:na]
Caused by: com.example.chat_service.domain.members.exceptions.MemberNotFoundError: Member not found
	at com.example.chat_service.infrastructure.persistence.members.repositories.MemberCommandOrmRepository.lambda$load$0(MemberCommandOrmRepository.java:118) ~[classes/:na]
	at java.base/java.util.Optional.orElseThrow(Optional.java:403) ~[na:na]
	at com.example.chat_service.infrastructure.persistence.members.repositories.MemberCommandOrmRepository.load(MemberCommandOrmRepository.java:118) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:190) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:158) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:135) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor$1.proceedWithInvocation(TransactionInterceptor.java:133) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:371) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:130) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:719) ~[spring-aop-7.0.7.jar:7.0.7]
	at com.example.chat_service.infrastructure.persistence.members.repositories.MemberCommandOrmRepository$$SpringCGLIB$$0.load(<generated>) ~[classes/:na]
	at com.example.chat_service.application.members.services.impl.MemberCommandServiceImpl.markAllRead(MemberCommandServiceImpl.java:380) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:190) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:158) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor$1.proceedWithInvocation(TransactionInterceptor.java:133) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:371) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:130) ~[spring-tx-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:719) ~[spring-aop-7.0.7.jar:7.0.7]
	at com.example.chat_service.application.members.services.impl.MemberCommandServiceImpl$$SpringCGLIB$$0.markAllRead(<generated>) ~[classes/:na]
	at com.example.chat_service.application.members.handlers.MemberCommandHandler.markAllRead(MemberCommandHandler.java:327) ~[classes/:na]
	at com.example.chat_service.api.chat.MemberController.markAllRead(MemberController.java:265) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:252) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:184) ~[spring-web-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:934) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:853) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:86) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:963) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:866) ~[spring-webmvc-7.0.7.jar:7.0.7]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1000) ~[spring-webmvc-7.0.7.jar:7.0.7]
	... 33 common frames omitted


) (// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/repositories/MemberCommandOrmRepository.java
package com.example.chat_service.infrastructure.persistence.members.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.domain.members.exceptions.InvalidMemberEntityError;
import com.example.chat_service.domain.members.exceptions.MemberNotFoundError;
import com.example.chat_service.domain.members.repositories.MemberCommandRepository;
import com.example.chat_service.infrastructure.persistence.members.MemberEntity;
import com.example.chat_service.infrastructure.persistence.members.MemberMapper;
import com.example.chat_service.infrastructure.persistence.members.jpa.MemberCommandJpaRepository;

/**
 * JPA/Hibernate implementation of {@link MemberCommandRepository}.
 *
 * <p>Handles write-side operations for Member aggregates using Spring Data JPA.
 * Leverages {@link MemberCommandJpaRepository} for persistence and {@link MemberMapper}
 * for domain ↔ entity conversion.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isLeft} is handled
 * explicitly via method names (e.g., {@code ...AndIsLeftFalse()}) rather than
 * {@code @SQLRestriction}. This allows the same repository to support both
 * active-only and all-members queries using pure ORM derivation.</p>
 *
 * <p><strong>Unique constraint:</strong> The {@code (user_id, room_id)} unique
 * constraint is enforced at the database level. Application logic should check
 * {@code existsByUserAndRoom()} before creation to avoid constraint violations.</p>
 *
 * <p><strong>Transaction management:</strong> All methods run within a transaction
 * via class-level {@code @Transactional}. Rollback occurs automatically on
 * unchecked exceptions, preserving aggregate consistency.</p>
 */
@Repository
@Transactional
public class MemberCommandOrmRepository implements MemberCommandRepository {

    private final MemberCommandJpaRepository memberJpaRepository;

    public MemberCommandOrmRepository(MemberCommandJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public void save(MemberAggregate aggregate) {
        MemberEntity entity = MemberMapper.aggregateToEntity(aggregate);
        
        try {
            // JPA merge pattern: save handles both insert and update
            // If entity with ID exists → UPDATE; otherwise → INSERT
            memberJpaRepository.save(entity);
            
        } catch (DataIntegrityViolationException e) {
            // Map database constraint violations to domain exceptions
            String errorMsg = e.getRootCause() != null 
                ? e.getRootCause().getMessage().toLowerCase() 
                : e.getMessage().toLowerCase();
            
            // Check for unique constraint violation on (user_id, room_id)
            if (errorMsg.contains("uk_members_user_room") || 
                (errorMsg.contains("user_id") && errorMsg.contains("room_id") && errorMsg.contains("unique"))) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    aggregate.member().userId(),
                    aggregate.member().roomId(),
                    "Database constraint violated: user can only have one membership per room"
                );
            }
            
            // Check for NOT NULL constraints on required fields
            if (errorMsg.contains("user_id") && errorMsg.contains("null")) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    null,
                    aggregate.member().roomId(),
                    "Database constraint violated: user_id cannot be null"
                );
            }
            if (errorMsg.contains("room_id") && errorMsg.contains("null")) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    aggregate.member().userId(),
                    null,
                    "Database constraint violated: room_id cannot be null"
                );
            }
            if (errorMsg.contains("status") && errorMsg.contains("null")) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    aggregate.member().userId(),
                    aggregate.member().roomId(),
                    "Database constraint violated: status cannot be null"
                );
            }
            
            // Re-throw as generic integrity error if no specific mapping
            throw new DataIntegrityViolationException(
                "Failed to persist member " + aggregate.member().id() + ": " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public MemberAggregate load(UUID memberId) {
        try {
            // Load by ID regardless of isLeft status (caller decides if they want active-only)
            MemberEntity entity = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundError(
                    memberId,
                    null,
                    null,
                    "Member not found"
                ));
            
            return MemberMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            MemberNotFoundError notFound = new MemberNotFoundError(
                memberId,
                null,
                null,
                "Member not found"
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public MemberAggregate loadByUserAndRoom(UUID userId, UUID roomId) {
        try {
            // Use active-only query (isLeft = false) for domain operations
            MemberEntity entity = memberJpaRepository.findByUserIdAndRoomIdAndIsLeftFalse(userId, roomId)
                .orElseThrow(() -> new MemberNotFoundError(
                    null,
                    userId,
                    roomId,
                    "No active membership found for user " + userId + " in room " + roomId
                ));
            
            return MemberMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            MemberNotFoundError notFound = new MemberNotFoundError(
                null,
                userId,
                roomId,
                "No active membership found for user " + userId + " in room " + roomId
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public Optional<MemberAggregate> loadByUserAndRoomOptional(UUID userId, UUID roomId) {
        // Use active-only query (isLeft = false) for domain operations
        return memberJpaRepository.findByUserIdAndRoomIdAndIsLeftFalse(userId, roomId)
            .map(MemberMapper::entityToAggregate);
    }

    @Override
    public boolean exists(UUID memberId) {
        // Check existence regardless of isLeft status
        return memberJpaRepository.existsById(memberId);
    }

    @Override
    public boolean existsByUserAndRoom(UUID userId, UUID roomId) {
        // Returns true if user has an ACTIVE membership in the room
        return memberJpaRepository.existsByUserIdAndRoomIdAndIsLeftFalse(userId, roomId);
    }

    // ── Bulk Load Operations ───────────────────────────────────────────

    @Override
    public List<MemberAggregate> bulkLoadByRoomId(UUID roomId) {
        // Load ALL members in room (including left) for admin/audit operations
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomId(roomId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByRoomId(UUID roomId) {
        // Load only active members for common read patterns
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomIdAndIsLeftFalse(roomId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadByUserId(UUID userId) {
        // Load ALL memberships for user (including left) for history/export
        List<MemberEntity> entities = memberJpaRepository.findAllByUserId(userId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByUserId(UUID userId) {
        // Load only active memberships for UI displays
        List<MemberEntity> entities = memberJpaRepository.findAllByUserIdAndIsLeftFalse(userId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        // Bulk lookup including left members for batch validation
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomIdAndUserIdIn(roomId, userIds);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        // Bulk lookup of active members only for messaging/presence features
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomIdAndUserIdInAndIsLeftFalse(roomId, userIds);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }
}) (// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/jpa/MemberCommandJpaRepository.java
package com.example.chat_service.infrastructure.persistence.members.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.members.MemberEntity;

/**
 * Spring Data JPA repository for command-side (write) operations on {@link MemberEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code MemberCommandOrmRepository} by providing
 * type-safe, derived-query methods for aggregate persistence and retrieval.
 * No custom JPQL/SQL — all methods use Spring Data JPA's method-name derivation.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isLeft} is handled
 * explicitly via method names (e.g., {@code ...AndIsLeftFalse()}) rather than
 * {@code @SQLRestriction}. This allows the same repository to support both
 * active-only and all-members queries using pure ORM derivation.</p>
 *
 * <p><strong>Unique constraint:</strong> The {@code (user_id, room_id)} unique
 * constraint is enforced at the database level. Application logic should check
 * existence before creation to avoid {@code DataIntegrityViolationException}.</p>
 *
 * <p><strong>Not for read-side queries:</strong> This repository is optimized for
 * loading full aggregates for mutation. For read-only views, lists, or projections,
 * use a separate query-side repository when implemented.</p>
 */
@Repository
public interface MemberCommandJpaRepository extends JpaRepository<MemberEntity, UUID> {

    // ── Inherited Methods from JpaRepository<MemberEntity, UUID> ─────────
    // Basic CRUD operations (no isLeft filtering — caller decides):
    //
    // • Optional<MemberEntity> findById(UUID id)
    //   → Loads entity by ID regardless of isLeft status
    //
    // • <S extends MemberEntity> S save(S entity)
    //   → INSERT if new ID, UPDATE if ID exists (JPA merge pattern)
    //
    // • boolean existsById(UUID id)
    //   → Fast existence check regardless of isLeft status

    // ── Derived Query Methods: Active Members Only (isLeft = false) ─────

    /**
     * Find an active member by user+room relationship.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code loadByUserAndRoom()} in command repository.</p>
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room
     * @return {@link Optional} containing the active member, or empty
     */
    Optional<MemberEntity> findByUserIdAndRoomIdAndIsLeftFalse(UUID userId, UUID roomId);

    /**
     * Check if an active membership exists for the given user+room pair.
     *
     * <p>Efficient existence check with {@code isLeft = false} filter.
     * Used by {@code existsByUserAndRoom()} in command repository.</p>
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room
     * @return {@code true} if an active membership exists
     */
    boolean existsByUserIdAndRoomIdAndIsLeftFalse(UUID userId, UUID roomId);

    /**
     * Load all active members in a room.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code bulkLoadActiveByRoomId()}.</p>
     *
     * @param roomId the UUID of the room
     * @return list of active member entities only
     */
    List<MemberEntity> findAllByRoomIdAndIsLeftFalse(UUID roomId);

    /**
     * Load all active memberships for a user across all rooms.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code bulkLoadActiveByUserId()}.</p>
     *
     * @param userId the UUID of the user
     * @return list of active member entities only
     */
    List<MemberEntity> findAllByUserIdAndIsLeftFalse(UUID userId);

    /**
     * Bulk lookup: find active members for multiple users in a specific room.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code bulkLoadActiveByUserIdsInRoom()}.</p>
     *
     * @param userIds collection of user UUIDs to lookup
     * @param roomId the UUID of the room
     * @return list of matching active member entities only
     */
    List<MemberEntity> findAllByRoomIdAndUserIdInAndIsLeftFalse(
            UUID roomId,
            Collection<UUID> userIds
    );

    // ── Derived Query Methods: All Members (including left) ─────────────

    /**
     * Load all members in a room, including those who have left.
     *
     * <p>No {@code isLeft} filter — returns all membership states.
     * Used by {@code bulkLoadByRoomId()} for admin/audit operations.</p>
     *
     * @param roomId the UUID of the room
     * @return list of all member entities (active + left)
     */
    List<MemberEntity> findAllByRoomId(UUID roomId);

    /**
     * Load all memberships for a user across all rooms, including left ones.
     *
     * <p>No {@code isLeft} filter — returns all membership states.
     * Used by {@code bulkLoadByUserId()} for user history/export operations.</p>
     *
     * @param userId the UUID of the user
     * @return list of all member entities (active + left)
     */
    List<MemberEntity> findAllByUserId(UUID userId);

    /**
     * Bulk lookup: find members for multiple users in a specific room (all states).
     *
     * <p>No {@code isLeft} filter — returns all matching memberships.
     * Used by {@code bulkLoadByUserIdsInRoom()} for batch validation.</p>
     *
     * @param userIds collection of user UUIDs to lookup
     * @param roomId the UUID of the room
     * @return list of matching member entities (active + left)
     */
    List<MemberEntity> findAllByRoomIdAndUserIdIn(
            UUID roomId,
            Collection<UUID> userIds
    );
}) or (// chat_service/src/main/java/com/example/chat_service/domain/members/MemberAggregate.java
package com.example.chat_service.domain.members;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ← Imports for exceptions in sub-package
import com.example.chat_service.domain.members.exceptions.InvalidMemberEntityError;
import com.example.chat_service.domain.members.exceptions.InvalidMemberStatusError;
import com.example.chat_service.domain.members.exceptions.InvalidUnreadMessagesError;
import com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError;
import com.example.chat_service.domain.members.exceptions.MemberStateTransitionError;
import com.example.chat_service.domain.members.exceptions.MemberUnauthorizedError;
import com.example.chat_service.domain.members.exceptions.MemberUnauthorizedErrorWithNoId;
import static java.util.Objects.requireNonNull;

/**
 * Aggregate root for managing the lifecycle and state of a Member.
 * Enforces business rules, coordinates state transitions, guards operations,
 * and validates ownership for user-initiated actions.
 */
public final class MemberAggregate {

    private Member member; // Mutable reference to current state; Member itself is immutable

    private MemberAggregate(Member member) {
        this.member = requireNonNull(member, "member cannot be null");
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public Member member() {
        return member;
    }

    // ── Factory Methods ──────────────────────────────────────────────

    /**
     * Create an aggregate from an existing Member entity (e.g., loaded from repository).
     */
    public static MemberAggregate fromEntity(Member member) {
        return new MemberAggregate(member);
    }

    /**
     * Create a new member aggregate with validation.
     * Fails fast on invalid input before entity creation.
     */
    public static MemberAggregate createNew(
            UUID id,
            UUID userId,
            UUID roomId,
            Member.Status initialStatus,
            LocalDateTime joinedAt
    ) {
        // Validate IDs
        if (id == null) {
            throw new InvalidMemberEntityError(null, userId, roomId, "Member ID cannot be null");
        }
        if (userId == null) {
            throw new InvalidMemberEntityError(id, null, roomId, "User ID cannot be null");
        }
        if (roomId == null) {
            throw new InvalidMemberEntityError(id, userId, null, "Room ID cannot be null");
        }
        if (initialStatus == null) {
            throw new InvalidMemberEntityError(id, userId, roomId, "Initial status cannot be null");
        }

        // Use provided timestamp or default to now
        LocalDateTime timestamp = joinedAt != null ? joinedAt : LocalDateTime.now();
        
        Member newMember = Member.create(id, userId, roomId, initialStatus);
        // Note: Member.create() uses LocalDateTime.now() internally.
        // If exact timestamp control is needed, use constructor directly via infrastructure.
        return new MemberAggregate(newMember);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static MemberAggregate createNew(
            UUID id,
            UUID userId,
            UUID roomId,
            Member.Status initialStatus
    ) {
        return createNew(id, userId, roomId, initialStatus, null);
    }

    /**
     * Convenience factory for creating a regular USER member.
     */
    public static MemberAggregate createNewAsUser(UUID id, UUID userId, UUID roomId) {
        return createNew(id, userId, roomId, Member.Status.USER);
    }

    /**
     * Convenience factory for creating an ADMIN member (e.g., room creator).
     */
    public static MemberAggregate createNewAsAdmin(UUID id, UUID userId, UUID roomId) {
        return createNew(id, userId, roomId, Member.Status.ADMIN);
    }

    // ── Business Operations ──────────────────────────────────────────

    /**
     * Promote this member to ADMIN status.
     * Application layer should verify caller has admin privileges before calling.
     */
    public MemberAggregate promote() {
        ensureActive("promote");
        
        if (member.isAdmin()) {
            throw new MemberStateTransitionError(
                member.id(),
                member.status().name(),
                Member.Status.ADMIN.name(),
                "Member is already an admin"
            );
        }
        
        this.member = member.promote();
        return this;
    }

    /**
     * Demote this member to USER status.
     * Application layer should verify caller has admin privileges before calling.
     */
    public MemberAggregate demote() {
        ensureActive("demote");
        
        if (!member.isAdmin()) {
            throw new MemberStateTransitionError(
                member.id(),
                member.status().name(),
                Member.Status.USER.name(),
                "Member is already a regular user"
            );
        }
        
        this.member = member.demote();
        return this;
    }

    /**
     * Increment unread messages count (system operation, e.g., new message delivered).
     * Does not require ownership check - typically called by message delivery service.
     */
    public MemberAggregate addUnreadMessages(int amount) {
        ensureActive("add_unread_messages");
        
        if (amount < 0) {
            throw new InvalidUnreadMessagesError(
                member.id(),
                member.unreadMessages(),
                amount,
                "increment amount cannot be negative"
            );
        }
        
        this.member = member.incrementUnreadMessages(amount);
        return this;
    }

    /**
     * Mark all messages as read.
     * Requires ownership - only the member themselves can clear their unread count.
     * @param requesterId ID of the user attempting this operation
     */
    public MemberAggregate markAllRead(UUID requesterId) {
        ensureActive("mark_all_read");
        ensureOwnership(requesterId, "mark_all_read");
        
        this.member = member.markAllRead();
        return this;
    }

    /**
     * Member voluntarily leaves the room.
     * Requires ownership - only the member can leave their own membership.
     * @param requesterId ID of the user attempting to leave
     */
    public MemberAggregate leave(UUID requesterId) {
        ensureActive("leave");
        ensureOwnership(requesterId, "leave");
        
        if (member.isLeft()) {
            throw new MemberStateTransitionError(
                member.id(),
                "active",
                "left",
                "Member has already left this room"
            );
        }
        
        this.member = member.leave();
        return this;
    }

    /**
     * Remove this member from the room (admin/system-initiated).
     * Application layer should verify caller has admin privileges before calling.
     * Does not use ensureOwnership - admins can remove any member.
     */
    public MemberAggregate remove() {
        ensureActive("remove");
        
        if (member.isLeft()) {
            throw new MemberStateTransitionError(
                member.id(),
                "active",
                "left",
                "Member has already left this room"
            );
        }
        
        this.member = member.remove();
        return this;
    }

    /**
     * Update the updated_at timestamp (e.g., for cache invalidation or heartbeat).
     * Requires ownership for user-initiated touches.
     * @param requesterId ID of the user performing the touch
     */
    public MemberAggregate touch(UUID requesterId) {
        ensureActive("touch");
        ensureOwnership(requesterId, "touch");
        
        this.member = member.touch();
        return this;
    }

    /**
     * Internal touch for system use (no ownership check).
     * Use sparingly - prefer explicit requesterId version for audit trails.
     */
    public MemberAggregate touchInternal() {
        ensureActive("touch_internal");
        this.member = member.touch();
        return this;
    }

    // ── State Queries (delegated to Member) ────────────────────────────

    public boolean isActive() {
        return member.isActive();
    }

    public boolean isAdmin() {
        return member.isAdmin();
    }

    public boolean hasUnreadMessages() {
        return member.hasUnreadMessages();
    }

    public UUID id() { return member.id(); }
    public UUID userId() { return member.userId(); }
    public UUID roomId() { return member.roomId(); }
    public Member.Status status() { return member.status(); }
    public int unreadMessages() { return member.unreadMessages(); }
    public LocalDateTime joinedAt() { return member.joinedAt(); }
    public boolean isLeft() { return member.isLeft(); }

    // ── Helper Methods ───────────────────────────────────────────────

    /**
     * Verify that the requester is the owner of this member record.
     * Throws MemberUnauthorizedError if IDs don't match.
     * @param requesterId ID of the user attempting the operation
     * @param operation Name of the operation for error context
     */
    private void ensureOwnership(UUID requesterId, String operation) {
        if (requesterId == null) {
            throw new MemberUnauthorizedErrorWithNoId(
                null,
                operation,
                "Requester ID cannot be null for ownership check"
            );
        }
        if (!requesterId.equals(member.userId())) {
            throw new MemberUnauthorizedError(
                member.id(),
                requesterId,
                operation,
                "User " + requesterId + " cannot perform '" + operation + "' on member record belonging to user " + member.userId()
            );
        }
    }

    private void ensureActive(String operation) {
        if (!member.isActive()) {
            throw new MemberOperationNotAllowedError(
                member.id(),
                operation,
                "Member is inactive or has left the room"
            );
        }
    }

    // ── Standard Object Methods ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberAggregate that)) return false;
        return Objects.equals(member.id(), that.member.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(member.id());
    }

    @Override
    public String toString() {
        return "MemberAggregate{" +
                "id=" + member.id() +
                ", userId=" + member.userId() +
                ", roomId=" + member.roomId() +
                ", status=" + member.status() +
                ", unreadMessages=" + member.unreadMessages() +
                ", isActive=" + isActive() +
                ", joinedAt=" + member.joinedAt() +
                '}';
    }
})(
    @Override
    public MemberAggregate leaveRoom(UUID memberId, UUID requesterId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.leave(requesterId);
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully left room (member_id={}, user_id={}, room_id={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for leave operation: member_id={}", memberId);
            throw e;

        } catch (MemberUnauthorizedError e) {
            logger.warn(
                "Leave operation unauthorized: member_id={}, actor_id={}, operation={}",
                e.getMemberId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Leave operation unauthorized (no member ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberStateTransitionError e) {
            logger.warn(
                "Leave operation failed: invalid state transition (member_id={}, current={}, target={}, reason={})",
                e.getMemberId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Leave operation not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Leave operation domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during leave operation: member_id={}", memberId, e);
            throw e;
        }
    })