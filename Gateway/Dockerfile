FROM python:3.10.2-alpine3.14
WORKDIR /usr/src/gateway

COPY requirements.txt ./
RUN pip install -r requirements.txt
COPY . .
ENV FLASK_APP=gateway.py
CMD flask run --host=0.0.0.0
