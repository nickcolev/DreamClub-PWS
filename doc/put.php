<?php
	define("CRLF", "\r\n");

function adump($a, $a2=NULL) {

	echo '<pre>';
	print_r($a);
	if($a2) { echo '<hr/>'; print_r($a2); }
	exit('</pre>');
}

function dbg($html) {

	$p = Array('/</', '/>/');
	$r = Array('&lt;', '&gt;');
	return preg_replace($p, $r, $html);
}

function form() {

	echo <<< HTML
<html><head><title>APWS test</title>
</head>
<body>

<form enctype="multipart/form-data" method="post">
 <input type="file" name="file"/>
 <input type="submit" value=" PUT "/><br/>
 Destination: <input name="dst" title="host:port[/path]" value="192.168.1.12:8080"/>
</form>

</body></html>
HTML;
}

function process($aFile, $dst) {
	if (!preg_match('|/$|', $dst)) $dst .= '/';
	preg_match_all('|(.*):(\d+)/(.*)|', $dst, $a);
	$host = $a[1][0]; $port = $a[2][0]; $file = $a[3][0].$aFile['name'];
	if (!($host & $port))
		exit('<p>Bad request<br/>connect '.$host.':'.$port.'<br/>target /'.$file.'</p>');
	$header	= 'PUT /'.$file.' HTTP/1.1'.CRLF.
		'Content-Type: application/octet-stream'.CRLF. 		//.$aFile['type'].CRLF.
		'Content-Length: '.$aFile['size'].CRLF.
		CRLF;
	$content = join('', file($aFile['tmp_name']));
	echo $host.':'.$port.'<pre>'.$header.'</pre>';
//adump($aFile);

	$fp = fsockopen($host, $port, $errno, $errstr, 30) or die($errstr);
	fwrite($fp, $header, strlen($header));
	fwrite($fp, $content, $aFile['size']);
	fclose($fp);
	echo '<hr/><p>Done.</p>';
}

	if($_FILES['file']['name'])
		process($_FILES['file'], $_POST['dst']);
	else
		form();
?>
