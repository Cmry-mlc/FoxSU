import{_ as i,o as a,c as n,a0 as e}from"./chunks/framework.DCluYYrN.js";const c=JSON.parse('{"title":"Integrate for non-GKI devices","description":"","frontmatter":{},"headers":[],"relativePath":"guide/how-to-integrate-for-non-gki.md","filePath":"guide/how-to-integrate-for-non-gki.md"}'),t={name:"guide/how-to-integrate-for-non-gki.md"};function l(p,s,h,k,d,r){return a(),n("div",null,[...s[0]||(s[0]=[e(`<h1 id="integrate-for-non-gki-devices" tabindex="-1">Integrate for non-GKI devices <a class="header-anchor" href="#integrate-for-non-gki-devices" aria-label="Permalink to &quot;Integrate for non-GKI devices&quot;">​</a></h1><div class="warning custom-block"><p class="custom-block-title">WARNING</p><p>This document is for archival reference only and is no longer maintained. Since KernelSU v1.0, we have dropped official support for non-GKI devices.</p></div><p>KernelSU can be integrated into non-GKI kernels and was backported to 4.14 and earlier versions.</p><p>Due to the fragmentation of non-GKI kernels, we don&#39;t have a universal way to build them; therefore, we cannot provide a non-GKI boot.img. However, you can build the kernel with KernelSU integrated on your own.</p><p>First, you should be able to build a bootable kernel from kernel source code. If the kernel isn&#39;t open source, then it is difficult to run KernelSU for your device.</p><p>If you&#39;re able to build a bootable kernel, there are two ways to integrate KernelSU into the kernel source code:</p><ol><li>Automatically with <code>kprobe</code></li><li>Manually</li></ol><h2 id="integrate-with-kprobe" tabindex="-1">Integrate with kprobe <a class="header-anchor" href="#integrate-with-kprobe" aria-label="Permalink to &quot;Integrate with kprobe&quot;">​</a></h2><p>KernelSU uses kprobe for its kernel hooks. If kprobe runs reliably on your kernel, we recommend integrating KernelSU this way.</p><p>First, add KernelSU to your kernel source tree:</p><div class="language-sh vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">sh</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">curl</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -LSs</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &quot;https://raw.githubusercontent.com/tiann/KernelSU/main/kernel/setup.sh&quot;</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> |</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> bash</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -s</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> v0.9.5</span></span></code></pre></div><div class="info custom-block"><p class="custom-block-title">INFO</p><p><a href="https://github.com/tiann/KernelSU/issues/1705" target="_blank" rel="noreferrer">KernelSU 1.0 and later versions no longer support non-GKI kernels</a>. The last supported version is <code>v0.9.5</code>, so make sure to use the correct version.</p></div><p>Then, you should check if kprobe is enabled in your kernel config. If it isn&#39;t, add these configs to it:</p><div class="language-txt vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">txt</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>CONFIG_KPROBES=y</span></span>
<span class="line"><span>CONFIG_HAVE_KPROBES=y</span></span>
<span class="line"><span>CONFIG_KPROBE_EVENTS=y</span></span></code></pre></div><p>Now, when you re-build your kernel, KernelSU should work correctly.</p><p>If you find that KPROBES is still not enabled, you can try enabling <code>CONFIG_MODULES</code>. If that doesn&#39;t solve the issue, use <code>make menuconfig</code> to search for other KPROBES dependencies.</p><p>However, if you encounter a bootloop after integrating KernelSU, this may indicate that the <strong>kprobe is broken in your kernel</strong>, which means that you should fix the kprobe bug or use another way.</p><div class="tip custom-block"><p class="custom-block-title">HOW TO CHECK IF KPROBE IS BROKEN？</p><p>Comment out <code>ksu_sucompat_init()</code> and <code>ksu_ksud_init()</code> in <code>KernelSU/kernel/ksu.c</code>. If the device boots normally, kprobe may be broken.</p></div><div class="info custom-block"><p class="custom-block-title">HOW TO GET MODULE UMOUNT FEATURE WORKING ON PRE-GKI?</p><p>If your kernel is older than 5.9, you should backport <code>path_umount</code> to <code>fs/namespace.c</code>. This is required to get &quot;Umount module&quot; feature work correctly. If you don&#39;t backport <code>path_umount</code>, &quot;Umount module&quot; feature won&#39;t work. You can get more info on how to achieve this at the end of this page.</p></div><h2 id="manually-modify-the-kernel-source" tabindex="-1">Manually modify the kernel source <a class="header-anchor" href="#manually-modify-the-kernel-source" aria-label="Permalink to &quot;Manually modify the kernel source&quot;">​</a></h2><p>If kprobe doesn&#39;t work on your kernel—either because of an upstream bug or because your kernel is older than 4.8—you can try the following approach:</p><p>First, add KernelSU to your kernel source tree:</p><div class="language-sh vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">sh</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">curl</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -LSs</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &quot;https://raw.githubusercontent.com/tiann/KernelSU/main/kernel/setup.sh&quot;</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> |</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> bash</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -s</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> v0.9.5</span></span></code></pre></div><p>Keep in mind that, on some devices, your defconfig may be located at <code>arch/arm64/configs</code> or in other cases, it may be at <code>arch/arm64/configs/vendor/your_defconfig</code>. Regardless of the defconfig you&#39;re using, make sure to enable <code>CONFIG_KSU</code> with <code>y</code> to enable or <code>n</code> to disable it. For example, if you choose to enable it, your defconfig should contain the following string:</p><div class="language-txt vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">txt</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span># KernelSU</span></span>
<span class="line"><span>CONFIG_KSU=y</span></span></code></pre></div><p>Next, add KernelSU calls to the kernel source. Below are some patches for reference:</p><div class="vp-code-group vp-adaptive-theme"><div class="tabs"><input type="radio" name="group-YWneH" id="tab-yOPUf6a" checked><label data-title="exec.c" for="tab-yOPUf6a">exec.c</label><input type="radio" name="group-YWneH" id="tab-Xv984YV"><label data-title="open.c" for="tab-Xv984YV">open.c</label><input type="radio" name="group-YWneH" id="tab-BpU7cVg"><label data-title="read_write.c" for="tab-BpU7cVg">read_write.c</label><input type="radio" name="group-YWneH" id="tab-n7QCJsp"><label data-title="stat.c" for="tab-n7QCJsp">stat.c</label></div><div class="blocks"><div class="language-diff vp-adaptive-theme active"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/exec.c b/fs/exec.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index ac59664eaecf..bdd585e1d2cc 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/exec.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/exec.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -1890,11 +1890,14 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static int __do_execve_file(int fd, struct filename *filename,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return retval;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern bool ksu_execveat_hook __read_mostly;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_execveat(int *fd, struct filename **filename_ptr, void *argv,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			void *envp, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_execveat_sucompat(int *fd, struct filename **filename_ptr,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+				 void *argv, void *envp, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static int do_execveat_common(int fd, struct filename *filename,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			      struct user_arg_ptr argv,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			      struct user_arg_ptr envp,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			      int flags)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (unlikely(ksu_execveat_hook))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_execveat(&amp;fd, &amp;filename, &amp;argv, &amp;envp, &amp;flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	else</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_execveat_sucompat(&amp;fd, &amp;filename, &amp;argv, &amp;envp, &amp;flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return __do_execve_file(fd, filename, argv, envp, flags, NULL);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span></code></pre></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/open.c b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 05036d819197..965b84d486b8 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -348,6 +348,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return ksys_fallocate(fd, mode, offset, len);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			 int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /*</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * access() needs to use the real uid/gid, not the effective uid/gid.</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * We do this by temporarily clearing all FS-related capabilities and</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -355,6 +357,7 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  */</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> long do_faccessat(int dfd, const char __user *filename, int mode)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	const struct cred *old_cred;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	struct cred *override_cred;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	struct path path;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	struct inode *inode;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	struct vfsmount *mnt;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int res;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = LOOKUP_FOLLOW;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_faccessat(&amp;dfd, &amp;filename, &amp;mode, NULL);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (mode &amp; ~S_IRWXO)	/* where&#39;s F_OK, X_OK, W_OK, R_OK? */</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EINVAL;</span></span></code></pre></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/read_write.c b/fs/read_write.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 650fc7e0f3a6..55be193913b6 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/read_write.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/read_write.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -434,10 +434,14 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> ssize_t kernel_read(struct file *file, void *buf, size_t count, loff_t *pos)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> EXPORT_SYMBOL(kernel_read);</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern bool ksu_vfs_read_hook __read_mostly;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_vfs_read(struct file **file_ptr, char __user **buf_ptr,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			size_t *count_ptr, loff_t **pos);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> ssize_t vfs_read(struct file *file, char __user *buf, size_t count, loff_t *pos)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	ssize_t ret;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (unlikely(ksu_vfs_read_hook))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_vfs_read(&amp;file, &amp;buf, &amp;count, &amp;pos);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (!(file-&gt;f_mode &amp; FMODE_READ))</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EBADF;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (!(file-&gt;f_mode &amp; FMODE_CAN_READ))</span></span></code></pre></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/stat.c b/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 376543199b5a..82adcef03ecc 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -148,6 +148,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_statx_fd(unsigned int fd, struct kstat *stat,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> EXPORT_SYMBOL(vfs_statx_fd);</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /**</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * vfs_statx - Get basic and extra attributes by filename</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * @dfd: A file descriptor representing the base dir for a relative filename</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -170,6 +172,7 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_statx(int dfd, const char __user *filename, int flags,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int error = -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = LOOKUP_FOLLOW | LOOKUP_AUTOMOUNT;</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_stat(&amp;dfd, &amp;filename, &amp;flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if ((flags &amp; ~(AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		       AT_EMPTY_PATH | KSTAT_QUERY_FLAGS)) != 0)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EINVAL;</span></span></code></pre></div></div></div><p>You should find the four functions in kernel source:</p><ol><li><code>do_faccessat</code>, usually in <code>fs/open.c</code></li><li><code>do_execveat_common</code>, usually in <code>fs/exec.c</code></li><li><code>vfs_read</code>, usually in <code>fs/read_write.c</code></li><li><code>vfs_statx</code>, usually in <code>fs/stat.c</code></li></ol><p>If your kernel doesn&#39;t have the <code>vfs_statx</code> function, use <code>vfs_fstatat</code> instead:</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/stat.c b/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 068fdbcc9e26..5348b7bb9db2 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -87,6 +87,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_fstat(unsigned int fd, struct kstat *stat)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> EXPORT_SYMBOL(vfs_fstat);</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_fstatat(int dfd, const char __user *filename, struct kstat *stat,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		int flag)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -94,6 +96,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_fstatat(int dfd, const char __user *filename, struct kstat *stat,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int error = -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = 0;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_stat(&amp;dfd, &amp;filename, &amp;flag);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if ((flag &amp; ~(AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		      AT_EMPTY_PATH)) != 0)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		goto out;</span></span></code></pre></div><p>For kernels eariler than 4.17, if you cannot find <code>do_faccessat</code>, just go to the definition of the <code>faccessat</code> syscall and place the call there:</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/open.c b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 2ff887661237..e758d7db7663 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -355,6 +355,9 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return error;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			        int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /*</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * access() needs to use the real uid/gid, not the effective uid/gid.</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * We do this by temporarily clearing all FS-related capabilities and</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -370,6 +373,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE3(faccessat, int, dfd, const char __user *, filename, int, mode)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int res;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = LOOKUP_FOLLOW;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_faccessat(&amp;dfd, &amp;filename, &amp;mode, NULL);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (mode &amp; ~S_IRWXO)	/* where&#39;s F_OK, X_OK, W_OK, R_OK? */</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EINVAL;</span></span></code></pre></div><h3 id="safe-mode" tabindex="-1">Safe Mode <a class="header-anchor" href="#safe-mode" aria-label="Permalink to &quot;Safe Mode&quot;">​</a></h3><p>To enable KernelSU&#39;s built-in Safe Mode, you should modify the <code>input_handle_event</code> function in <code>drivers/input/input.c</code>:</p><div class="tip custom-block"><p class="custom-block-title">TIP</p><p>It&#39;s strongly recommended to enable this feature, it&#39;s very useful for preventing bootloops!</p></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/drivers/input/input.c b/drivers/input/input.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 45306f9ef247..815091ebfca4 100755</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/drivers/input/input.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/drivers/input/input.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -367,10 +367,13 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static int input_get_disposition(struct input_dev *dev,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return disposition;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern bool ksu_input_hook __read_mostly;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_input_handle_event(unsigned int *type, unsigned int *code, int *value);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static void input_handle_event(struct input_dev *dev,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			       unsigned int type, unsigned int code, int value)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">	int disposition = input_get_disposition(dev, type, code, &amp;value);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (unlikely(ksu_input_hook))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_input_handle_event(&amp;type, &amp;code, &amp;value);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   #endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (disposition != INPUT_IGNORE_EVENT &amp;&amp; type != EV_SYN)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		add_input_randomness(type, code, value);</span></span></code></pre></div><div class="info custom-block"><p class="custom-block-title">ENTERING SAFE MODE ACCIDENTALLY?</p><p>If you&#39;re using manual integration and don&#39;t disable <code>CONFIG_KPROBES</code>, the user will be able to trigger Safe Mode by pressing the volume down button after booting! Therefore, if you&#39;re using manual integration, it&#39;s necessary to disable <code>CONFIG_KPROBES</code>!</p></div><h3 id="failed-to-execute-pm-in-terminal" tabindex="-1">Failed to execute <code>pm</code> in terminal? <a class="header-anchor" href="#failed-to-execute-pm-in-terminal" aria-label="Permalink to &quot;Failed to execute \`pm\` in terminal?&quot;">​</a></h3><p>You should modify <code>fs/devpts/inode.c</code>. Reference:</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/devpts/inode.c b/fs/devpts/inode.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 32f6f1c68..d69d8eca2 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/devpts/inode.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/devpts/inode.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -602,6 +602,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> struct dentry *devpts_pty_new(struct pts_fs_info *fsi, int index, void *priv)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">        return dentry;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_devpts(struct inode*);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+#endif</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /**</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * devpts_get_priv -- get private data for a slave</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * @pts_inode: inode of the slave</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -610,6 +612,7 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> struct dentry *devpts_pty_new(struct pts_fs_info *fsi, int index, void *priv)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  */</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> void *devpts_get_priv(struct dentry *dentry)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+       #ifdef CONFIG_KSU</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+       ksu_handle_devpts(dentry-&gt;d_inode);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+       #endif</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">        if (dentry-&gt;d_sb-&gt;s_magic != DEVPTS_SUPER_MAGIC)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">                return NULL;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">        return dentry-&gt;d_fsdata;</span></span></code></pre></div><h3 id="how-to-backport-path-umount" tabindex="-1">How to backport path_umount <a class="header-anchor" href="#how-to-backport-path-umount" aria-label="Permalink to &quot;How to backport path_umount&quot;">​</a></h3><p>You can make the &quot;Umount modules&quot; feature work on pre-GKI kernels by manually backporting <code>path_umount</code> from 5.9. You can use this patch as reference:</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/namespace.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/namespace.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -1739,6 +1739,39 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static inline bool may_mandlock(void)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> #endif</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+static int can_umount(const struct path *path, int flags)</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+{</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	struct mount *mnt = real_mount(path-&gt;mnt);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (flags &amp; ~(MNT_FORCE | MNT_DETACH | MNT_EXPIRE | UMOUNT_NOFOLLOW))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		return -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (!may_mount())</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		return -EPERM;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (path-&gt;dentry != path-&gt;mnt-&gt;mnt_root)</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		return -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (!check_mnt(mnt))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		return -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (mnt-&gt;mnt.mnt_flags &amp; MNT_LOCKED) /* Check optimistically */</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		return -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (flags &amp; MNT_FORCE &amp;&amp; !capable(CAP_SYS_ADMIN))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		return -EPERM;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	return 0;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+}</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+int path_umount(struct path *path, int flags)</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+{</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	struct mount *mnt = real_mount(path-&gt;mnt);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	int ret;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ret = can_umount(path, flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (!ret)</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ret = do_umount(mnt, flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	/* we mustn&#39;t call path_put() as that would clear mnt_expiry_mark */</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	dput(path-&gt;dentry);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	mntput_no_expire(mnt);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	return ret;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+}</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /*</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * Now umount can handle mount points as well as block devices.</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * This is important for filesystems which use unnamed block devices.</span></span></code></pre></div><p>Finally, build your kernel again, and KernelSU should work correctly.</p>`,45)])])}const E=i(t,[["render",l]]);export{c as __pageData,E as default};
