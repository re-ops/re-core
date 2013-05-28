{
  :capistrano {
    :src "git://github.com/narkisr/cap-demo.git"
    :operates-on "redis"
    :actions {
       :deploy  {:args ["deploy" "-s" "hostname=~{hostname}"]}
       :restart {:args ["restart" "-s" "hostname=~{hostname}"]}
     }
  }
}
