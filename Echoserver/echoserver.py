import socketio

sio = socketio.Client()


@sio.event
def connect_error(data):
    print("The connection failed!")

@sio.event
def disconnect():
    print("I'm disconnected!")
    
@sio.on('connect')
def on_connect():
    print("I am connected!")
    
sio.connect('http://192.168.2.9:5000')


