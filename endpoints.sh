
curl --request POST -H "Content-Type: application/json" \
    -c cookies.txt \
    -d '{"login": "testuser", "password": "testpass"}' \
    localhost:3000/api/login

curl --request POST -H "Content-Type: application/json" \
    -d '{"name": "Bob-123", "message": "123123123123123"}' \
    localhost:3000/api/message

curl -b cookies.txt localhost:3000/api/session
