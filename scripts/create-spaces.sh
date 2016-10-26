#!/bin/bash

set -ex

SCRIPT="s = box.schema.space.create('tester', {id = 1, temporary = true})
s:create_index('primary', {
                            type = 'hash',
                            parts = {1, 'unsigned'}
                          })
s:create_index('name', {
                         type = 'hash',
                         parts = {2, 'string', 3, 'string'}
                       })
"
echo "log=require('log')\n log.error('lol')\n" > tarantoolctl connect tarantool:3301 2>&1
