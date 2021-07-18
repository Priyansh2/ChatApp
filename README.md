# ChatApp
A rudimentary messaging application based on client-server architecture for communication amongst multiple connected clients.

# Project Scope
* Built a chat room for N users based on Half-Duplex communication model using socket programming in Java
* Implement built-in commands (see problem.pdf for detail)
* Implement TCP and UDP oriented message sending and file sharing features
* Error handling with appropriate display message


# Commands
- [x] create chatroom <chatroom_name> **- command to create chatroom**
- [x] list chatrooms **- command to list all chat rooms**
- [x] join <chatroom_name> **- command to join existing chat room**
- [x] leave **-command to leave existing chat room**
- [x] list users **-command to list all users in a chatroom**
- [x] add <user_name> **-commmand to add another user to chat room**
- [x] reply <message_content> **-command to send message in a chatroom**
- [x] reply <file_path> <mode> **- command to send any kind of file in a chatroom through TCP/UDP mode**

# Usage
```bash
Run server: java server.java <max_users>
Run client: java client.java <user_name>
```
# TODO
- [ ] Full-Duplex transmission mode
- [ ] End-to-end Encryption  
- [ ] GUI for better display
