import flask
import redis
from flask import Flask, request, render_template, jsonify
from flask_socketio import SocketIO, send
#import socketio


app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret'
#app.config['DEBUG'] = True
socketio = SocketIO(app)

r = redis.Redis(host = 'localhost',port= '6379')

#find IP of echoservers or probes
def find_ip_address():
    if request.headers.getlist("X-Forwarded-For"):
        return request.headers.getlist("X-Forwarded-For")[0]
    else:
        return request.remote_addr

#when an echoserver connects
@socketio.on('connect')
def on_connect():
    ip_address = find_ip_address()
    print(ip_address + " has connected")
    r.hset("echos", ip_address, "Connected") #add echoserver to redis
    
@socketio.on('message')
def on_message(msg):
    print(msg)

#when echoserver disconnects
@socketio.on('disconnect')
def on_disconnect():
    ip_address = find_ip_address()
    r.hdel("echos", ip_address) #remove it from redis
    print(ip_address + ' has disconnected')   
 
 #when a probe connects   
@app.route('/', methods=['GET'])
def index():
    ip_address = find_ip_address()
    print("Android device " + ip_address)
    return "Your IP address is: " + ip_address
  
#send the probes all the IPs of echoservers    
@app.route('/get_data', methods=['GET'])
def get_data():
    allkeys = r.hkeys("echos")
    allkeys_str = []
    for i in allkeys:
        x = i.decode()
        allkeys_str.append(x)
    listToStr = ','.join([str(elem) for elem in allkeys_str])
    return jsonify(allkeys_str)
    
#collects all data a probe sent    
@app.route('/post_data', methods=['POST'])
def post_data():
    ip_address = find_ip_address()
    value=request.form['value']
    times = {}
    i = value.replace("{","")
    i = i.replace("}","")
    for x in i.split(", "):
        k, z = x.split("=")
        times[k] = z
    for IP, time in times.items():
        r.hset(ip_address, IP, time)  #add data to redis under probe's IP
    print(value)
    return value
    
    
if __name__=="__main__":
    socketio.run(app, host='0.0.0.0')
