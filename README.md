# XBall
client-server multi-player soccer game for Android made with Java+libGDX

The game engine runs on both desktop Java and Android.
However, it was designed with touch UI, so it is not playable on the desktop. Rather, the desktop runs the headless version without an OpenGL/graphics context.

This game uses the principle of client-side prediction. Each client maintains a connection to the server, and each frame, the control state of the client is sent to the server.
At a regular interval, the server serializes the game engine state and sends it to all clients. Each client will load the engine at the given frame/tick count, and then use its buffer of local client control state to fast-forward from the authoritative server state to the present.
