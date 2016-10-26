
s = box.schema.space.create('tester', {id = 1, temporary = true})

s:create_index('primary', {

                            type = 'hash',

                            parts = {1, 'unsigned'}

                          })

s:create_index('name', {

                         type = 'hash',

                         parts = {2, 'string', 3, 'string'}

                       })

 0 3301
