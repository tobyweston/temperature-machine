#!/bin/mksh
rcsid='$MirOS: contrib/hosted/tg/deb/mkdebidx.sh,v 1.77 2019/05/18 18:39:07 tg Exp $'
#-
# Copyright © 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015,
#	      2016, 2017, 2019
#	mirabilos <m@mirbsd.org>
#
# Provided that these terms and disclaimer and all copyright notices
# are retained or reproduced in an accompanying document, permission
# is granted to deal in this work without restriction, including un-
# limited rights to use, publicly perform, distribute, sell, modify,
# merge, give away, or sublicence.
#
# This work is provided "AS IS" and WITHOUT WARRANTY of any kind, to
# the utmost extent permitted by applicable law, neither express nor
# implied; without malicious intent or gross negligence. In no event
# may a licensor, author or contributor be held liable for indirect,
# direct, other damage, loss, or other issues arising in any way out
# of dealing in the work, even if advised of the possibility of such
# damage or existence of a defect, except proven that it results out
# of said person's immediate fault when using the work as intended.

unset normarchs repo_keyid gpg_remote gpg_bin repo_origin repo_label repo_title
unset -f repo_intro repo_description
me=$(dirname "$0"); [[ -s $me/mkdebidx.inc ]] && . "$me/mkdebidx.inc"
unset me

[[ -n ${normarchs+x} ]] || set -A normarchs -- i386
# either '' (locally) or 'remsign user@host.domain.com' (remote ssh)
[[ -n ${repo_keyid+x} ]] || repo_keyid=0xAA917C6F
[[ -n ${gpg_remote+x} ]] || gpg_remote=
case ${gpg_bin:-x} {
(gpg|gpg1|gpg2|gnupg|gnupg1|gnupg2) ;;
(*)
	if [[ -n $gpg_remote ]]; then
		gpg_bin=gpg
	elif ! gpg_bin=$(whence -p gpg1); then
		gpg_bin=gpg
	fi
	;;
}
[[ -n ${repo_origin+x} ]] || repo_origin='The MirOS Project'
[[ -n ${repo_label+x} ]] || repo_label=wtf
[[ -n ${repo_title+x} ]] || repo_title='MirDebian “WTF” Repository'
typeset -f repo_intro >/dev/null || function repo_intro {
	cat <<-'EOF'
	<p>This APT repository contains packages by mirabilos (<i>wtf</i>)
	 for use with the Debian operating system and its derivates. It is
	 not affiliated with Debian. Some of the content is merely hosted
	 for people close to MirBSD or Debian; some is affiliated with The
	 MirOS Project.<br /><i>Debian</i> is a registered trademark owned
	 by Software in the Public Interest, Inc.<br />“The MirOS Project”
	 and “MirBSD” are unregistered trademarks owned by mirabilos.</p>
EOF
}
typeset -f repo_description >/dev/null || function repo_description {
	typeset suite_nick=$1

	print -nr -- "WTF ${suite_nick} Repository"
}
set -A dpkgarchs -- alpha amd64 arm arm64 armel armhf hppa hurd-i386 i386 \
    ia64 kfreebsd-amd64 kfreebsd-i386 m68k mips mips64el mipsel powerpc \
    powerpcspe ppc64 ppc64el s390 s390x sh4 sparc sparc64 x32
[[ -n "${normarchs[*]}" ]] || set -A normarchs -- "${dpkgarchs[@]}"

set +U
export LC_ALL=C
unset LANGUAGE
typeset -Z11 -Uui16 hv

function remsign {
	target=$1; shift
	master=remsign.ctl$$
	tmpfnm=remsign.tmp$$
	ssh -fNM -o ControlPath=$tmpfnm "$target"
	ssh -o ControlPath=$tmpfnm "$target" cat \>$tmpfnm
	ssh -o ControlPath=$tmpfnm -t "$target" "$* $tmpfnm" 0<&2 1>&2
	rv=$?
	ssh -o ControlPath=$tmpfnm "$target" "cat $tmpfnm.asc; rm -f $tmpfnm $tmpfnm.asc"
	ssh -o ControlPath=$tmpfnm "$target" -O exit
	return $rv
}

function die {
	local rv=1

	if [[ $1 = +([0-9]) ]]; then
		rv=$1
		shift
	fi
	print -ru2 -- "E: $*"
	exit $rv
}

function checkedhash {
	if [[ $1 = size ]]; then
		REPLY=$(stat -c '%s' "$2")
		[[ $REPLY = +([0-9]) ]] || die "Error getting size of '$2'"
	else
		set -o noglob
		set -A REPLY -- $($1 "$2")
		set +o noglob
		[[ $REPLY = +([0-9a-f]) ]] || die "Error getting $1 of '$2'"
	fi
}

function putfile {
	tee $1 | gzip -n9 >$1.gz
}

function sortlist {
	typeset x u=$1

	if [[ $u = -u ]]; then
		shift
	else
		u=
	fi

	for x in "$@"; do
		print -r -- "$x"
	done | sort $u
}

# escape XHTML characters (three mandatory XML ones plus double quotes,
# the latter in an XML safe fashion numerically though)
function xhtml_escape {
	if (( $# )); then
		print -nr -- "$@"
	else
		cat
	fi | sed \
	    -e 's&\&amp;g' \
	    -e 's<\&lt;g' \
	    -e 's>\&gt;g' \
	    -e 's"\&#34;g'
}

cd "$(dirname "$0")"
rm -f dpkg_578162_workaround

IFS=:; set -o noglob
dpkgarchl=:all:"${dpkgarchs[*]}":
IFS=$' \t\n'; set +o noglob

suites=:
for suite in "$@"; do
	suites=:dists/$suite$suites
done

allsuites=
for suite in dists/*; do
	allsuites="$allsuites${allsuites:+ }${suite##*/}"
	[[ -h $suite ]] && continue
	[[ $suites = : || $suites = *:"$suite":* ]] || continue
	archs=
	distribution=
	dcodename=
	debootstrap_compat=0
	. $suite/distinfo.sh
	suitearchs=${archs:-${normarchs[*]}}
	components=Components:
	for dist in $suite/*; do
		[[ -d $dist/. ]] || continue
		rm -rf $dist/binary-* $dist/source
		ovf= oef= osf= om=-m
		(( debootstrap_compat )) && om=
		[[ -s $dist/override.file ]] && ovf=$dist/override.file
		[[ -s $dist/override.extra ]] && oef="-e $dist/override.extra"
		[[ -s $dist/override.src ]] && osf="-s $dist/override.src"
		components="$components ${dist##*/}"
		archs=
		[[ -s $dist/distinfo.sh ]] && . $dist/distinfo.sh
		set -A distarchs -- $(sortlist -u all ${archs:-$suitearchs})
		IFS=:; set -o noglob
		distarchl=:"${distarchs[*]}":
		IFS=$' \t\n'; set +o noglob
		nmds=0
		for arch in $(sortlist -u ${distarchs[*]} ${dpkgarchs[*]}) /; do
			# put "all" last
			[[ $arch = all ]] && continue
			[[ $arch = / ]] && arch=all
			# create index
			if [[ $dpkgarchl != *:"$arch":* ]]; then
				die "Invalid arch '$arch' in $dist"
			elif [[ $distarchl != *:"$arch":* ]]; then
				print "\n===> Linking all =>" \
				    "${dist#dists/}/$arch/Packages"
				ln -s binary-all $dist/binary-$arch
			elif [[ $arch = all ]] && (( nmds == 1 )); then
				print "\n===> Linking $firstmd =>" \
				    "${dist#dists/}/all/Packages"
				ln -s binary-$firstmd $dist/binary-all
			else
				print "\n===> Creating" \
				    "${dist#dists/}/$arch/Packages\n"
				mkdir -p $dist/binary-$arch
				(dpkg-scanpackages $oef $om -a $arch \
				    $dist $ovf || \
				    echo $? >$dist/binary-$arch/failed) | \
				    putfile $dist/binary-$arch/Packages
				[[ -e $dist/binary-$arch/failed ]] && \
				    exit $(<$dist/binary-$arch/failed)
				(( nmds++ )) || firstmd=$arch
			fi
		done
		print "\n===> Creating ${dist#dists/}/Sources"
		mkdir -p $dist/source
		[[ -e dpkg_578162_workaround ]] || (dpkg-scansources $oef $osf \
		    $dist $ovf || touch dpkg_578162_workaround) | \
		    putfile $dist/source/Sources
		[[ -e dpkg_578162_workaround ]] && (dpkg-scansources $osf \
		    $dist $ovf || echo $? >$dist/source/failed) | \
		    putfile $dist/source/Sources
		[[ -e $dist/source/failed ]] && exit $(<$dist/source/failed)
		print done.
		print "\n===> Creating ${dist#dists/}/i18n/Index"
		[[ -d $dist/i18n/. ]] || mkdir -p $dist/i18n
		[[ -d $dist/i18n/. ]] || die "Cannot create $dist/i18n"
		rm -f $dist/i18n/.done
		(cd $dist/i18n
		tfiles=/
		[[ -h .hashcache || ! -f .hashcache ]] && rm -rf .hashcache
		for ent in .* *; do
			[[ $ent = . || $ent = .. || $ent = .hashcache ]] && continue
			[[ -h $ent || -e $ent ]] || continue
			if [[ ! -f $ent || $ent != Translation-* ]]; then
				rm -rf "$ent"
				continue
			fi
			ent=${ent#Translation-}
			ent=${ent%.bz2}
			[[ $tfiles = */"$ent"/* ]] || tfiles+=$ent/
		done
		[[ -e .hashcache ]] || :>.hashcache
		if [[ $tfiles = / ]]; then
			:>Translation-tlh_DE
			tfiles=/tlh_DE/
		fi
		print SHA1: >Index
		IFS=/; set -o noglob
		set -A tflist -- ${tfiles#/}
		IFS=$' \t\n'; set +o noglob
		for ent in "${tflist[@]}"; do
			ent=Translation-$ent
			if [[ $ent -nt $ent.bz2 ]]; then
				if ! bzip2 -9 <"$ent" >"$ent.bz2"; then
					rm -f "$ent.bz2"
					die "bzip2 '$ent' died"
				fi
			elif [[ -e $ent ]]; then
				rm -f "$ent"
			fi
			hash=${|checkedhash sha1sum "$ent.bz2";} || exit 1
			hnum=0
			grep "^$hash " .hashcache |&
			while read -p hsha1 hsize hmd5 hsha2 usha1 usize umd5 usha2; do
				[[ $hsha1 = "$hash" ]] || continue
				hnum=1
				while read -p hsha1 x; do
					# flush coprocess, look for dupes
					[[ $hsha1 = "$hash" ]] && hnum=2
				done
				break
			done
			hsha1=$hash
			if (( hnum != 1 )); then
				[[ -e $ent ]] || \
				    if ! bzip2 -d <"$ent.bz2" >"$ent"; then
					rm -f "$ent"
					die "bzip2 '$ent.bz2' died"
				fi
				umd5=${|checkedhash md5sum "$ent";} || exit 1
				hmd5=${|checkedhash md5sum "$ent.bz2";} || exit 1
				usha1=${|checkedhash sha1sum "$ent";} || exit 1
				usha2=${|checkedhash sha256sum "$ent";} || exit 1
				hsha2=${|checkedhash sha256sum "$ent.bz2";} || exit 1
				usize=${|checkedhash size "$ent";} || exit 1
				hsize=${|checkedhash size "$ent.bz2";} || exit 1
				(( hnum )) || print $hsha1 $hsize $hmd5 $hsha2 $usha1 $usize $umd5 $usha2 >>.hashcache
			fi
			[[ -e $ent ]] && rm -f "$ent"
			print -u4 $hsha1 $hsize $hmd5 $hsha2 $usha1 $usize $umd5 $usha2
			print -ru5 " $hsha1 $hsize $ent.bz2"
			print -ru6 " $umd5 $usize ${dist##*/}/i18n/$ent"
			print -ru6 " $hmd5 $hsize ${dist##*/}/i18n/$ent.bz2"
			print -ru7 " $usha1 $usize ${dist##*/}/i18n/$ent"
			print -ru7 " $hsha1 $hsize ${dist##*/}/i18n/$ent.bz2"
			print -ru8 " $usha2 $usize ${dist##*/}/i18n/$ent"
			print -ru8 " $hsha2 $hsize ${dist##*/}/i18n/$ent.bz2"
		done 4>.hashcache.new 5>>Index 6>.hashcache.md5 7>.hashcache.sha1 8>.hashcache.sha2
		rm -f .hashcache
		mv -f .hashcache.new .hashcache
		:>.done)
		[[ -e $dist/i18n/.done ]] || die i18n generation unsuccessful
		rm -f $dist/i18n/.done
		print done.
	done
	print "\n===> Creating ${suite#dists/}/Release"
	rm -f $suite/Release-*
	xdone=$(realpath $suite/Release-done)
	(cat <<-EOF
		Origin: ${repo_origin}
		Label: ${repo_label}
		Suite: ${distribution:-${suite##*/}}
		Codename: ${dcodename:-${suite##*/}}
		Date: $(date -Ru)
		Architectures: all ${dpkgarchs[*]} source
		$components
		Description: $(repo_description "$nick")
		MD5Sum:
	EOF
	exec 4>$suite/Release-sha1
	exec 5>$suite/Release-sha2
	print -u4 SHA1:
	print -u5 SHA256:
	cd $suite
	set -A cache_fn
	set -A cache_md5
	set -A cache_sha1
	set -A cache_sha2
	set -A cache_size
	for n in Contents-* */{binary-*,i18n,source}/{Index,{Packag,Sourc}es*}; do
		[[ -f $n ]] || continue
		# realpath-ise $n and cache the checksum
		nn=$(realpath "$n")
		#XXX once mksh can, use associative arrays instead
		hv=16#${nn@#}
		# simple hash collision solver by increment
		nc=${cache_fn[hv]}
		while [[ -n $nc && $nc != "$nn" ]]; do
			nc=${cache_fn[++hv]}
		done
		if [[ $nc = "$nn" ]]; then
			nm=${cache_md5[hv]}
			ns=${cache_size[hv]}
			nsha1=${cache_sha1[hv]}
			nsha2=${cache_sha2[hv]}
		else
			# GNU *sum tools are horridly inefficient
			nm=${|checkedhash md5sum "$nn";} || exit 1
			nsha1=${|checkedhash sha1sum "$nn";} || exit 1
			nsha2=${|checkedhash sha256sum "$nn";} || exit 1
			ns=${|checkedhash size "$nn";} || exit 1
			cache_md5[hv]=$nm
			cache_size[hv]=$ns
			cache_fn[hv]=$nn
			cache_sha1[hv]=$nsha1
			cache_sha2[hv]=$nsha2
		fi
		print " $nm $ns $n"
		print -u4 " $nsha1 $ns $n"
		print -u5 " $nsha2 $ns $n"
		if [[ $n = */i18n/Index ]]; then
			n=${n%Index}
			cat "${n}.hashcache.md5"
			cat >&4 "${n}.hashcache.sha1"
			cat >&5 "${n}.hashcache.sha2"
			rm -f "${n}.hashcache."*
		fi
	done
	:>"$xdone") >$suite/Release-tmp
	[[ -e $xdone ]] || die Release generation died
	cat $suite/Release-sha1 $suite/Release-sha2 >>$suite/Release-tmp

	# note: InRelease files can only be safely used by jessie and up.
	unset use_inrelease
	. $suite/distinfo.sh
	rm -f $suite/InRelease $suite/Release $suite/Release.gpg
	if [[ $use_inrelease = 1 ]]; then
		$gpg_remote $gpg_bin -u $repo_keyid --no-comment --clearsign \
		    <$suite/Release-tmp >$suite/Release-inl
		mv -f $suite/Release-inl $suite/InRelease
	else
		$gpg_remote $gpg_bin -u $repo_keyid --no-comment -sab \
		    <$suite/Release-tmp >$suite/Release-sig
		mv -f $suite/Release-tmp $suite/Release
		mv -f $suite/Release-sig $suite/Release.gpg
	fi
	rm -f $suite/Release-*
done

print "\n===> Creating debidx.htm\n"

set -A preplsrc
set -A prepldst
integer nsrc=0 nbin=0 nrpl=0
br='<br />'

# syntax:	${suitename}/${distname}/${pN}/${pp} <suite>
# example:	sid/wtf/openntpd/i386 lenny
# not here:	squeeze/wtf/xz-utils/% backport-source
# binary-only?	sid/wtf/pbuilder/= something
if [[ -s mkdebidx.lnk ]]; then
	while read pn pd; do
		[[ $pn = '#'* ]] && continue
		if [[ $pn != +([a-z0-9_])/+([a-z0-9_-])/+([!/])/@(%|=|+([a-z0-9])) || \
		    $pd != +([a-z0-9_]) ]]; then
			print -u2 "W: Invalid lnk line '$pn' '$pd'"
			continue
		fi
		preplsrc[nrpl]=$pn
		prepldst[nrpl++]=$pd
	done <mkdebidx.lnk
fi

for suite in dists/*; do
	[[ -h $suite ]] && continue
	for dist in $suite/*; do
		[[ -d $dist/. ]] || continue
		suitename=${suite##*/}
		if [[ $suitename != +([a-z0-9_]) ]]; then
			print -u2 "W: Invalid suite name '$suitename'"
			continue 2
		fi
		distname=${dist##*/}
		if [[ $distname != +([a-z0-9_-]) ]]; then
			print -u2 "W: Invalid dist name '$distname'"
			continue
		fi

		gzip -dc $dist/source/Sources.gz |&
		pn=; pv=; pd=; pp=; Lf=
		while IFS= read -pr line; do
			case $line {
			(" "*)
				if [[ -n $Lf ]]; then
					eval x=\$$Lf
					x=$x$line
					eval $Lf=\$x
				fi
				;;
			("Package: "*)
				pn=${line##Package:*([	 ])}
				Lf=pn
				;;
			("Version: "*)
				pv=${line##Version:*([	 ])}
				Lf=pv
				;;
			("Binary: "*)
				pd=${line##Binary:*([	 ])}
				Lf=pd
				;;
			("Directory: "*)
				pp=${line##Directory:*([	 ])}
				Lf=pp
				;;
			(?*)	# anything else
				Lf=
				;;
			(*)	# empty line
				if [[ -n $pn && -n $pv && -n $pd && -n $pp ]]; then
					i=0
					while (( i < nsrc )); do
						[[ ${sp_name[i]} = "$pn" && \
						    ${sp_dist[i]} = "$distname" ]] && break
						let i++
					done
					if (( i == nsrc )); then
						let nsrc++
						pvo=
						ppo=
					else
						eval pvo=\$\{sp_ver_${suitename}[i]\}
						eval ppo=\$\{sp_dir_${suitename}[i]\}
					fi
					sp_name[i]=$pn
					sp_dist[i]=$distname
					#sp_suites[i]="${sp_suites[i]} $suitename"
					if (( nrpl )); then
						x=${suitename}/${distname}/${pn}/source
						j=0
						while (( j < nrpl )); do
							[[ ${preplsrc[j]} = "$x" ]] && break
							let j++
						done
						(( j < nrpl )) && pv="${pv}from ${prepldst[j]}"
					fi
					eval sp_ver_${suitename}[i]='${pvo:+$pvo,}$pv'
					eval sp_dir_${suitename}[i]='${ppo:+$ppo,}$pp/'
					sp_desc[i]=${sp_desc[i]},$pd
				fi
				pn=; pv=; pd=; pp=; Lf=
				;;
			}
		done

		gzip -dc $(for f in $dist/binary-*/Packages.gz; do
			[[ -e $f ]] || continue
			realpath "$f"
		done | sort -u) |&
		pn=; pv=; pd=; pp=; pN=; pf=; pABP=; Lf=
		while IFS= read -pr line; do
			case $line {
			(" "*)
				if [[ -n $Lf ]]; then
					eval x=\$$Lf
					x=$x$line
					eval $Lf=\$x
				fi
				;;
			("Package: "*)
				pN=${line##Package:*([	 ])}
				Lf=pN
				;;
			("Source: "*)
				pn=${line##Source:*([	 ])}
				pn=${pn%% *}
				Lf=pn
				;;
			("Version: "*)
				pv=${line##Version:*([	 ])}
				Lf=pv
				;;
			("Description: "*)
				pd=${line##Description:*([	 ])}
				;;
			("Architecture: "*)
				pp=${line##Architecture:*([	 ])}
				Lf=pp
				;;
			("Filename: "*)
				pf=${line##Filename:*([	 ])}
				Lf=pf
				;;
			("Auto-Built-Package: "*)
				pABP=${line##Auto-Built-Package:*([	 ])}
				Lf=pABP
				;;
			(?*)	# anything else
				Lf=
				;;
			(*)	# empty line
				[[ $pf = *:* || $pf = *'%'* ]] && \
				    die Illegal character in $dist \
				    packages $pp "'Filename: $pf'"
				[[ -n $pn ]] || pn=$pN
				if [[ $pN = *-dbgsym && $pABP = debug-symbols ]]; then
					: skip
				elif [[ -n $pn && -n $pv && -n $pd && -n $pp ]]; then
					i=0
					while (( i < nbin )); do
						[[ ${bp_disp[i]} = "$pN" && ${bp_desc[i]} = "$pd" && \
						    ${bp_dist[i]} = "$distname" ]] && break
						let i++
					done
					(( i == nbin )) && let nbin++
					bp_name[i]=$pn
					bp_disp[i]=$pN
					bp_dist[i]=$distname
					#bp_suites[i]="${bp_suites[i]} $suitename"
					if (( nrpl )); then
						x=${suitename}/${distname}/${pN}/${pp}
						j=0
						while (( j < nrpl )); do
							[[ ${preplsrc[j]} = "$x" ]] && break
							let j++
						done
						(( j < nrpl )) && pv="from ${prepldst[j]}"
					fi
					[[ -n $pf ]] && pv="<a href=\"$pf\">$pv</a>"
					pv="$pp: $pv"
					eval x=\${bp_ver_${suitename}[i]}
					[[ $br$x$br = *"$br$pv$br"* ]] || x=$x${x:+$br}$pv
					eval bp_ver_${suitename}[i]=\$x
					bp_desc[i]=$pd
				fi
				pn=; pv=; pd=; pp=; pN=; pf=; pABP=; Lf=
				;;
			}
		done
	done
done

(cat <<'EOF'
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
 "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en"><head>
 <meta http-equiv="content-type" content="text/html; charset=utf-8" />
 <meta name="MSSmartTagsPreventParsing" content="TRUE" />
EOF
print -r -- " <title>${repo_title} Index</title>"
[[ -s NEWS.rss ]] && print '<link rel="alternate" type="application/rss+xml" title="RSS" href="NEWS.rss" />'
cat <<'EOF'
 <style type="text/css"><!--/*--><![CDATA[/*><!--*/
  table {
   border: 1px solid black;
   border-collapse: collapse;
   text-align: left;
   vertical-align: top;
  }
  tr {
   border: 1px solid black;
   text-align: left;
   vertical-align: top;
  }
  td {
   border: 1px solid black;
   text-align: left;
   vertical-align: top;
  }
  th {
   background-color: #000000;
   color: #FFFFFF;
  }
  .tableheadcell {
   border: 1px solid #999999;
   padding: 3px;
   white-space: nowrap;
  }
  .srcpkgline {
   background-color: #CCCCCC;
  }
  .srcpkgdist {
   background-color: #666666;
   color: #FFFFFF;
   font-weight: bold;
  }
  .binpkgdist {
   background-color: #999999;
   color: #FFFFFF;
   font-weight: bold;
  }
 /*]]>*/--></style>
</head><body>
EOF
print -r -- "<h1>${repo_title}</h1>"
repo_intro
cat <<'EOF'
<p><a href="dists/">Browse</a> the repository or read about how to amend <a
 href="sources.txt">/etc/apt/sources.list</a> in order to use it.
EOF
[[ -s 0-NOTE.txt ]] && print ' Also read my <a href="0-NOTE.txt">notes</a>.'
[[ -s NEWS.rss ]] && print ' There is an <a href="NEWS.rss">RSS newsfeed</a>.'
cat <<EOF
 This repository uses <a
  href="http://pgp.uni-mainz.de:11371/pks/lookup?search=${repo_keyid}&amp;op=vindex">${repo_keyid}</a>
 as signing key.
</p>
<h2>Suites</h2>
<ul>
EOF

allsuites=$(for suitename in $allsuites; do
	print $suitename
done | sort -u)

for suitename in $allsuites; do
	suite=dists/$suitename
	if [[ -h $suite ]]; then
		ent=$(realpath "$suite")
		n=$(realpath dists)
		[[ $ent = "$n"/+([!/]) ]] || continue
		ent=${ent#"$n"/}
		for n in $allsuites; do
			[[ $n = "$ent" ]] && break
		done
		[[ $n = "$ent" ]] && print -r \
		    " <li>$suitename: symbolic link to $ent</li>"
		continue
	fi
	. $suite/distinfo.sh
	print -n " <li>${suite##*/}: <a href=\"$suite/\">$desc</a> (dists:"
	for dist in $suite/*; do
		[[ -d $dist/. ]] || continue
		distname=${dist##*/}
		print -n " <a href=\"$suite/$distname/\">$distname</a>"
	done
	print ")</li>"
done
print "</ul>"
print "<h2>Packages</h2>"
print "<table width=\"100%\"><thead>"
print "<tr class=\"tablehead\">"
print " <th class=\"tableheadcell\">dist</th>"
print " <th class=\"tableheadcell\" rowspan=\"2\">Binary / Description</th>"
for suitename in $allsuites; do
	[[ -h dists/$suitename ]] && continue
	print " <th class=\"tableheadcell\" rowspan=\"2\">$suitename</th>"
done
print "</tr><tr class=\"tablehead\">"
print " <th class=\"tableheadcell\">package name</th>"
print "</tr></thead><tbody>"

set -A bp_sort
i=0
while (( i < nbin )); do
	print $i ${bp_disp[i++]} #${bp_suites[i]}
done | sort -k2 |&
while read -p num rest; do
	bp_sort[${#bp_sort[*]}]=$num
done

i=0
while (( i < nsrc )); do
	print $i ${sp_name[i++]}
done | sort -k2 |&
while read -p num rest; do
	print "\n<!-- sp #$num = ${sp_name[num]} -->"
	print "<tr class=\"srcpkgline\">"
	print " <td class=\"srcpkgdist\">${sp_dist[num]}</td>"
	pd=
	for x in $(tr ', ' '\n' <<<"${sp_desc[num]}" | sort -u); do
		[[ -n $x ]] && pd="$pd, $x"
	done
	print " <td rowspan=\"2\" class=\"srcpkgdesc\">${pd#, }</td>"
	for suitename in $allsuites; do
		[[ -h dists/$suitename ]] && continue
		eval pvo=\${sp_ver_${suitename}[num]}
		eval ppo=\${sp_dir_${suitename}[num]}
		IFS=,; set -o noglob
		set -A pva -- $pvo
		set -A ppa -- $ppo
		IFS=$' \t\n'; set +o noglob
		(( ${#pva[*]} )) || pva[0]=
		y=
		i=0
		while (( i < ${#pva[*]} )); do
			pv=${pva[i]}
			pp=${ppa[i]}
			if [[ $pv = *""* ]]; then
				pvdsc=${pv%%""*}
				pv=${pv##*""}
			else
				pvdsc=$pv
			fi
			if [[ -z $pv ]]; then
				pv=-
				if (( nrpl )); then
					x=${suitename}/${sp_dist[num]}/${sp_name[num]}/%
					j=0
					while (( j < nrpl )); do
						[[ ${preplsrc[j]} = "$x" ]] && break
						let j++
					done
					(( j < nrpl )) && pv=${prepldst[j]}
				fi
			elif [[ $pp != ?(/) ]]; then
				pv="<a href=\"$pp${sp_name[num]}_${pvdsc##+([0-9]):}.dsc\">$pv</a>"
			fi
			[[ $pp != ?(/) ]] && pv="<a href=\"$pp\">[dir]</a> $pv"
			y=${y:+"$y<br />"}$pv
			let i++
		done
		print " <td rowspan=\"2\" class=\"srcpkgitem\">$y</td>"
	done
	print "</tr><tr class=\"srcpkgline\">"
	print " <td class=\"srcpkgname\">${sp_name[num]}</td>"
	print "</tr>"
	k=0
	while (( k < nbin )); do
		(( (i = bp_sort[k++]) < 0 )) && continue
		[[ ${bp_name[i]} = "${sp_name[num]}" && \
		    ${bp_dist[i]} = "${sp_dist[num]}" ]] || continue
		bp_sort[k - 1]=-1
		#print "<!-- bp #$i for${bp_suites[i]} -->"
		print "<!-- bp #$i -->"
		print "<tr class=\"binpkgline\">"
		print " <td class=\"binpkgname\">${bp_disp[i]}</td>"
		print " <td class=\"binpkgdesc\">$(xhtml_escape "${bp_desc[i]}")</td>"
		for suitename in $allsuites; do
			[[ -h dists/$suitename ]] && continue
			eval pv=\${bp_ver_${suitename}[i]}
			if [[ -z $pv ]]; then
				pv=-
				if (( nrpl )); then
					x=${suitename}/${sp_dist[num]}/${sp_name[num]}/%
					j=0
					while (( j < nrpl )); do
						[[ ${preplsrc[j]} = "$x" ]] && break
						let j++
					done
					(( j < nrpl )) && pv=${prepldst[j]}
				fi
			fi
			print " <td class=\"binpkgitem\">$pv</td>"
		done
		print "</tr>"
	done
done

num=0
for i in ${bp_sort[*]}; do
	(( i < 0 )) && continue
	if (( !num )); then
		print "\n<!-- sp ENOENT -->"
		print "<tr class=\"srcpkgline\">"
		print " <td class=\"srcpkgname\">~ENOENT~</td>"
		print " <td class=\"srcpkgdesc\">binary" \
		    "packages without a matching source package</td>"
		for suitename in $allsuites; do
			[[ -h dists/$suitename ]] && continue
			print " <td class=\"srcpkgitem\">-</td>"
		done
		print "</tr>"
		num=1
	fi
	#print "<!-- bp #$i for${bp_suites[i]} -->"
	print "<!-- bp #$i -->"
	print "<tr class=\"binpkgline\">"
	print " <td class=\"binpkgdist\">${bp_dist[i]}</td>"
	print " <td rowspan=\"2\" class=\"binpkgdesc\">$(xhtml_escape "${bp_desc[i]}")</td>"
	for suitename in $allsuites; do
		[[ -h dists/$suitename ]] && continue
		eval pv=\${bp_ver_${suitename}[i]}
		if [[ -z $pv ]]; then
			pv=-
			if (( nrpl )); then
				x=${suitename}/${bp_dist[num]}/${bp_disp[num]}/=
				j=0
				while (( j < nrpl )); do
					[[ ${preplsrc[j]} = "$x" ]] && break
					let j++
				done
				(( j < nrpl )) && pv=${prepldst[j]}
			fi
		fi
		print " <td rowspan=\"2\" class=\"binpkgitem\">$pv</td>"
	done
	print "</tr><tr class=\"binpkgline\">"
	print " <td class=\"binpkgname\">${bp_disp[i]}</td>"
	print "</tr>"
done

cat <<EOF

</tbody></table>

<p>• <a href="http://validator.w3.org/check/referer">Valid XHTML/1.1!</a>
 • <small>Generated on $(date -u +'%F %T') by <tt
 style="white-space:pre;">$rcsid</tt></small> •</p>
</body></html>
EOF

:) >debidx.htm
print done.