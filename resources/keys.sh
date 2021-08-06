#!/bin/bash

import_key() {
   gpg --receive-keys --keyserver keyserver.ubuntu.com $1
}

# current key 
import_key A624671CE2135D9F71596F38BE1C522AC629222D
# old key
import_key 7083BA46F0B2460C
# me.raynes
import_key 115E8C72AE1DFBFDD4D8786BA56A26A672B08826
# com.palletops/stevedore
import_key AFEDB040C1E8CE259F8B4B153DDA1B3EC890F586
# org.apache.httpcomponents/httpclient
import_key 0785B3EFF60B1B1BEA94E0BB7C25280EAE63EBE5
# net.java.dev.jna/jna
import_key FA7929F83AD44C4590F6CC6815C71C0A4E0B8EDD
# org.libvirt/libvirt
import_key 1B933CE798B6985C8D79975DE1A36FB910E9E138
# less-awful-ssl
import_key 6722D1BB1AFFC51AC43452E7161EC240CC48018B
# cheshire
import_key 6CA9E3B29F28FEA86750B6BE9D6465D43ACECAE0
# com.mikesamuel/json-sanitizer
import_key F55EF5BB19F52A250FEDC0DF39450183608E49D4
# com.hierynomus/sshj
import_key 379CE192D401AB61
# org.zeromq/jeromq 
import_key 174F88318B64CB02
# com.brunobonacci/safely 
import_key F05B1D2EF3BED8C8E12F44EF6BEF76A7B805F61B
# com.google.guava/guava
import_key ABE9F3126BB741C1
# commons-codec
import_key 21939FF0CA2A6567
# mount
import_key 148AA196DF8D6332
# io.aviso/pretty
import_key 26406BB1AA04110E49AA8671A82090FF7CC19136
# riemann-clojure-client
import_key 5CECAE951A380FC6C6982FE8361953C9DEB28012
# expound
import_key 725F73F2BF6D0DEDAE758599DB3DCB7A484504A5
# juxt/dirwatch
import_key D7EAC082D28BC79233279A3206CEE45EC93B3AF9
# serializable-fn
import_key AF567B9777E77DDC
