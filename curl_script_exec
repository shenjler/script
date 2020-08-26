echo "hello world" > /root/hello.txt

echo $1
echo $2

# curl http://foo.com/script.sh | bash -s arg1 arg2
curl https://raw.githubusercontent.com/shenjler/scripts/master/hello.sh | sh -s -- arg1 arg2


# @See https://hant-kb.kutu66.com/bash/post_5251878
#curl http://foo.com/script.sh | bash
#bash <<( curl http://foo.com/script.sh )


# curl http://example.com/script.sh | bash -s -- arg1 arg2
# 注意两个破折号(-- --)，它告诉bash不要将任何内容作为bash的参数进行处理。# 

# 这样它就可以处理任何类型的参数，e.g.:# 

# curl -L http://bootstrap.saltstack.org | bash -s -- -M -N stable
# 这当然可以通过标准输入来处理任何类型的输入，而不仅仅是 curl，所以你可以确认它通过echo使用简单的BASH脚本输入来工作：# 

# echo 'i=1; for a in $@; do echo "$i = $a"; i=$((i+1)); done' | sh -s -- -a1 -a2 -a3 --long some_text
