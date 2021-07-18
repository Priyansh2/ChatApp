# MessagingApp
A rudimentary messaging application based on client-server architecture for communication amongst multiple connected clients.

# Project Scope
* Built a chat room for N users based on Half-Duplex communication model using socket programming in Java
* Implement built-in commands (see problem.pdf for detail)
* Implement TCP and UDP oriented message sending and file sharing features
* Error handling with appropriate display message


# Commands
- [ ] create chatroom <chatroom_name> **- command to create chatroom**
- [ ] list chatrooms **- command to list all chat rooms**
- [ ] join <chatroom_name> **- command to join existing chat room**
- [ ] leave **-command to leave existing chat room**
- [ ] list users **-command to list all users in a chatroom**
- [ ] add <user_name> **-commmand to add another user to chat room**

# Usage
```bash
Run server: java server.java <max_users>
Run client: java client.java <user_name>
```
