# nginx 安装upload模块

# https://blog.csdn.net/qq_24027457/article/details/89161730


# 1.yum安装nginx
rpm -ivh http://nginx.org/packages/centos/7/noarch/RPMS/nginx-release-centos-7-0.el7.ngx.noarch.rpm

# 2.查看nginx信息
yum info nginx  (查看nginx版本 方便后面下载对应版本)

# 3.yum安装nginx
yum -y install nginx 

# 4.查看yum 安装的nginx 相关参数
nginx -V



# 5.nginx 启动、停止、重启
systemctl start nginx #启动 nginx 服务
systemctl stop nginx #停止 nginx 服务
systemctl restart nginx #重启 nginx 服务

# 6.启动检查是否启动成功
curl -i localhost
 
#显示如下证明启动成功
···
<h1>Welcome to nginx!</h1>
···
安装三方模块
其实yum安装nginx 后想要添加第三方模块，只需对yum安装的nginx相同版本的源码进行编译后替换


# 1.安装源码包需要的依赖
yum -y install gcc gcc-c++ make libtool zlib zlib-devel openssl openssl-devel pcre pcre-devel

# 2.下载对应的源码
通过nginx -V 可以知道yum 安装nginx 的版本为1.16.1,下载对应的源码

cd /opt
wget http://nginx.org/download/nginx-1.16.1.tar.gz

# 下载upload模块
git clone https://github.com/fdintino/nginx-upload-module.git

# 3.查看对应configure
nginx -V
tar xf nginx-1.16.1.tar.gz
cd nginx-1.16.1
nginx -V
configure arguments:   --prefix=/usr/share/nginx \
--sbin-path=/usr/sbin/nginx \
--modules-path=/usr/lib64/nginx/modules \
--conf-path=/etc/nginx/nginx.conf \
--error-log-path=/var/log/nginx/error.log \
--http-log-path=/var/log/nginx/access.log \
--http-client-body-temp-path=/var/lib/nginx/tmp/client_body \
--http-proxy-temp-path=/var/lib/nginx/tmp/proxy \
--http-fastcgi-temp-path=/var/lib/nginx/tmp/fastcgi \
--http-uwsgi-temp-path=/var/lib/nginx/tmp/uwsgi \
--http-scgi-temp-path=/var/lib/nginx/tmp/scgi \
--pid-path=/var/run/nginx.pid \
--lock-path=/var/lock/subsys/nginx \
...
...
--with-ld-opt=' -Wl,-E'

# 4.增加对应的模块
./configure  --prefix=/usr/share/nginx \
--sbin-path=/opt/nginx \
--modules-path=/usr/lib64/nginx/modules \
--conf-path=/etc/nginx/nginx.conf \
--error-log-path=/var/log/nginx/error.log \
--http-log-path=/var/log/nginx/access.log \
--http-client-body-temp-path=/var/lib/nginx/tmp/client_body \
--http-proxy-temp-path=/var/lib/nginx/tmp/proxy \
--http-fastcgi-temp-path=/var/lib/nginx/tmp/fastcgi \
--http-uwsgi-temp-path=/var/lib/nginx/tmp/uwsgi \
--http-scgi-temp-path=/var/lib/nginx/tmp/scgi \
--pid-path=/var/run/nginx.pid \
...
...
--add-module=/opt/nginx-upload-module
 
#切记编译安装时 --sbin-path=/opt/nginx   一定要跟yum安装的sbin路径不一样，稍后做替换

# 5.编译安装
make && make install

# 6.对文件进行替换
cp /usr/sbin/nginx /usr/sbin/nginx.bak #备份
cp /opt/nginx /usr/sbin/nginx #替换
systemctl restart nginx #重启 nginx 服务

# 7.查看替换后的nginx模块



# 8.显然，已经将upload模块安装ok



# 安装
#   yum install libxml2-devel libxslt-devel
#   yum -y install perl-devel perl-ExtUtils-Embed
#   yum install gperftools


# 原来：
--prefix=/usr/share/nginx 	\
--sbin-path=/usr/sbin/nginx 	\
--modules-path=/usr/lib64/nginx/modules 	\
--conf-path=/etc/nginx/nginx.conf 	\
--error-log-path=/var/log/nginx/error.log 	\
--http-log-path=/var/log/nginx/access.log 	\
--http-client-body-temp-path=/var/lib/nginx/tmp/client_body 	\
--http-proxy-temp-path=/var/lib/nginx/tmp/proxy 	\
--http-fastcgi-temp-path=/var/lib/nginx/tmp/fastcgi 	\
--http-uwsgi-temp-path=/var/lib/nginx/tmp/uwsgi 	\
--http-scgi-temp-path=/var/lib/nginx/tmp/scgi 	\
--pid-path=/run/nginx.pid 	\
--lock-path=/run/lock/subsys/nginx 	\
--user=nginx 	\
--group=nginx 	\
--with-file-aio 	\
--with-ipv6 	\
--with-http_ssl_module 	\
--with-http_v2_module 	\
--with-http_realip_module 	\
--with-stream_ssl_preread_module 	\
--with-http_addition_module 	\
--with-http_xslt_module=dynamic 	\
--with-http_image_filter_module=dynamic 	\
--with-http_sub_module 	\
--with-http_dav_module 	\
--with-http_flv_module 	\
--with-http_mp4_module 	\
--with-http_gunzip_module 	\
--with-http_gzip_static_module 	\
--with-http_random_index_module 	\
--with-http_secure_link_module 	\
--with-http_degradation_module 	\
--with-http_slice_module 	\
--with-http_stub_status_module 	\
--with-http_perl_module=dynamic 	\
--with-http_auth_request_module 	\
--with-mail=dynamic 	\
--with-mail_ssl_module 	\
--with-pcre 	\
--with-pcre-jit 	\
--with-stream=dynamic 	\
--with-stream_ssl_module 	\
--with-google_perftools_module 	\
--with-debug 	\
--with-cc-opt='-O2 -g -pipe -Wall -Wp,-D_FORTIFY_SOURCE=2 -fexceptions -fstack-protector-strong 	\
--param=ssp-buffer-size=4 -grecord-gcc-switches -specs=/usr/lib/rpm/redhat/redhat-hardened-cc1 -m64 -mtune=generic' 	\
--with-ld-opt='-Wl,-z,relro -specs=/usr/lib/rpm/redhat/redhat-hardened-ld -Wl,-E'

# 替换
./configure  --prefix=/opt/share/nginx 	\
--sbin-path=/opt/nginx 	\
--modules-path=/usr/lib64/nginx/modules 	\
--conf-path=/etc/nginx/nginx.conf 	\
--error-log-path=/var/log/nginx/error.log 	\
--http-log-path=/var/log/nginx/access.log 	\
--http-client-body-temp-path=/var/lib/nginx/tmp/client_body 	\
--http-proxy-temp-path=/var/lib/nginx/tmp/proxy 	\
--http-fastcgi-temp-path=/var/lib/nginx/tmp/fastcgi 	\
--http-uwsgi-temp-path=/var/lib/nginx/tmp/uwsgi 	\
--http-scgi-temp-path=/var/lib/nginx/tmp/scgi 	\
--pid-path=/run/nginx.pid 	\
--lock-path=/run/lock/subsys/nginx 	\
--user=nginx 	\
--group=nginx 	\
--with-file-aio 	\
--with-ipv6 	\
--with-http_ssl_module 	\
--with-http_v2_module 	\
--with-http_realip_module 	\
--with-stream_ssl_preread_module 	\
--with-http_addition_module 	\
--with-http_xslt_module=dynamic 	\
--with-http_image_filter_module=dynamic 	\
--with-http_sub_module 	\
--with-http_dav_module 	\
--with-http_flv_module 	\
--with-http_mp4_module 	\
--with-http_gunzip_module 	\
--with-http_gzip_static_module 	\
--with-http_random_index_module 	\
--with-http_secure_link_module 	\
--with-http_degradation_module 	\
--with-http_slice_module 	\
--with-http_stub_status_module 	\
--with-http_perl_module=dynamic 	\
--with-http_auth_request_module 	\
--with-mail=dynamic 	\
--with-mail_ssl_module 	\
--with-pcre 	\
--with-pcre-jit 	\
--with-stream=dynamic 	\
--with-stream_ssl_module 	\
--with-google_perftools_module 	\
--with-debug 	\
--with-cc-opt='-O2 -g -pipe -Wall -Wp,-D_FORTIFY_SOURCE=2 -fexceptions -fstack-protector-strong 	\
--param=ssp-buffer-size=4 -grecord-gcc-switches -specs=/usr/lib/rpm/redhat/redhat-hardened-cc1 -m64 -mtune=generic' 	\
--with-ld-opt='-Wl,-z,relro -specs=/usr/lib/rpm/redhat/redhat-hardened-ld -Wl,-E' \
--add-module=/opt/nginx-upload-module


# 上传模块使用方法
# https://github.com/fdintino/nginx-upload-module
# https://blog.csdn.net/zzhongcy/article/details/88863037
# https://www.nginx.org.cn/plug/detail/162


    location /_upload {
        default_type text/plain;
        return 200 "hello upload"
    }
        # 转到后台处理URL,表示Nginx接收完上传的文件后，然后交给后端处理的地址
        upload_pass @clubServer;

        # Store files to this directory
        # The directory is hashed, subdirectories 0 1 2 3 4 5 6 7 8 9 should exist
        upload_store /data/file/upload;


        # 上传文件的权限，rw表示读写 r只读
        upload_store_access user:rw group:rw all:rw;
        set $upload_field_name "file";
        
        # Set specified fields in request body
        upload_set_form_field "${upload_field_name}_name" $upload_file_name;
        upload_set_form_field "${upload_field_name}_content_type" $upload_content_type;
        upload_set_form_field "${upload_field_name}_path" $upload_tmp_path;

        # Inform backend about hash and size of a file
        upload_aggregate_form_field "${upload_field_name}_md5" $upload_file_md5;
        upload_aggregate_form_field "${upload_field_name}_size" $upload_file_size;

        # 允许的字段，允许全部可以 "^.*$"
        upload_pass_form_field "^.*$";
        # upload_pass_form_field "^submit$|^description$";
        # 每秒字节速度控制，0表示不受控制，默认0, 128K
        upload_limit_rate 0;
        # 如果pass页面是以下状态码，就删除此次上传的临时文件
        upload_cleanup 400 404 499 500-505;
        # 打开开关，意思就是把前端脚本请求的参数会传给后端的脚本语言，比如：http://192.168.1.251:9000/upload/?k=23,后台可以通过POST['k']来访问。
        upload_pass_args on;  
    }

    location @clubServer {
        rewrite ^(.*)$ /club/test/ break;
        proxy_pass http://localhost:8080;
        # return 200 $arg_echostr;  # 如果不需要后端程序处理，直接返回200即可
    }



# 34.92.52.42 执行
curl -i -X POST -F "file=@/root/result.txt" "http://www.shenjl.club/_upload"
curl -i -X POST -F  "file=@/root/abc.txt" "http://www.shenjl.club/_upload"
curl -i -X POST -F  "file=@/root/abc.txt" "http://www.shenjl.club/_upload"

curl "http://www.shenjl.club/upload/2/"


#  若为 upload_store /data/file/upload 1; 必须初始化目录,
cd /data/file/upload
mkdir -p 0 1 2 3 4 5 6 7 8 9
chown -R nginx:nginx /data/file/upload
du -hs /data/file/upload/*