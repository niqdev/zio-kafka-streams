#!/bin/bash

CURRENT_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd -P)
cd ${CURRENT_PATH}

##############################

PARAM_DATA_NAME=${1:?"Missing DATA_NAME"}
DATA_PATH="${CURRENT_PATH}/../data"

##############################

echo "[+] format_data"

echo "[*] DATA_NAME=${PARAM_DATA_NAME}"

DATA_KEY=$(cat "${DATA_PATH}/${PARAM_DATA_NAME}"-key.json | jq -c)
DATA_VALUE=$(cat "${DATA_PATH}/${PARAM_DATA_NAME}"-value.json | jq -c)

echo -e "\n$DATA_KEY:::$DATA_VALUE\n"

echo "[-] format_data"
