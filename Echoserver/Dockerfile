FROM python:3.10.2-alpine3.14
WORKDIR /usr/src/echoserver

COPY requirements.txt ./
RUN pip install -r requirements.txt
COPY . .
