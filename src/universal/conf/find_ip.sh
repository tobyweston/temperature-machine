# Pi specific function to try and find the externally facing IP address
getIpAddress() {
    local eth0=`grep "eth0" /proc/net/dev`
    if  [ -n "eth0" ] ; then
       local lan="eth0"
    else
       local lan="wlan0"
    fi
    echo $( ip -f inet addr show ${lan} | grep -Po 'inet \K[\d.]+' )
}

