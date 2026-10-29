import{_ as i,o as a,c as n,a0 as t}from"./chunks/framework.DCluYYrN.js";const E=JSON.parse('{"title":"如何為非 GKI 核心整合 KernelSU","description":"","frontmatter":{},"headers":[],"relativePath":"zh_TW/guide/how-to-integrate-for-non-gki.md","filePath":"zh_TW/guide/how-to-integrate-for-non-gki.md"}'),e={name:"zh_TW/guide/how-to-integrate-for-non-gki.md"};function l(p,s,h,k,d,c){return a(),n("div",null,[...s[0]||(s[0]=[t(`<h1 id="how-to-integrate-kernelsu-for-non-gki-kernels" tabindex="-1">如何為非 GKI 核心整合 KernelSU <a class="header-anchor" href="#how-to-integrate-kernelsu-for-non-gki-kernels" aria-label="Permalink to &quot;如何為非 GKI 核心整合 KernelSU {#how-to-integrate-kernelsu-for-non-gki-kernels}&quot;">​</a></h1><div class="warning custom-block"><p class="custom-block-title">WARNING</p><p>該文件僅供存檔參考，不再維護更新。 自 KernelSU v1.0 版本之後，我們放棄了對非 GKI 裝置的官方支援。</p></div><p>KernelSU 可以被整合到非 GKI 核心中，現在它最低支援到核心 4.14 版本；理論上也可以支援更低的版本。</p><p>由於非 GKI 核心的片段化極其嚴重，因此通常沒有統一的方法來建置它，所以我們也無法為非 GKI 裝置提供 Boot 映像。但您完全可以自行整合 KernelSU 並建置核心以繼續使用。</p><p>首先，您必須有能力從您裝置的核心原始碼建置出一個可以開機並且能夠正常使用的核心，如果核心並非開放原始碼，這通常難以做到。</p><p>如果您已經做好了上述準備，那有兩個方法來將 KernelSU 整合至您的核心之中。</p><ol><li>藉助 <code>kprobe</code> 自動整合</li><li>手動修改核心原始碼</li></ol><h2 id="integrate-with-kprobe" tabindex="-1">使用 kprobe 整合 <a class="header-anchor" href="#integrate-with-kprobe" aria-label="Permalink to &quot;使用 kprobe 整合 {#integrate-with-kprobe}&quot;">​</a></h2><p>KernelSU 使用 kprobe 機制來處理核心的相關 hook，如果 <em>kprobe</em> 可以在您建置的核心中正常運作，那麼建議使用這個方法進行整合。</p><p>首先，把 KernelSU 新增至您的核心來源樹狀結構，再核心的根目錄執行以下命令：</p><div class="language-sh vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">sh</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">curl</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -LSs</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &quot;https://raw.githubusercontent.com/tiann/KernelSU/main/kernel/setup.sh&quot;</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> |</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> bash</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -s</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> v0.9.5</span></span></code></pre></div><div class="info custom-block"><p class="custom-block-title">公告</p><p><a href="https://github.com/tiann/KernelSU/issues/1705" target="_blank" rel="noreferrer">KernelSU 1.0 及更新版本不再支援非 GKI 核心</a>。最後一個支援的版本為 <code>v0.9.5</code>，請確保使用的版本正確。</p></div><p>然後，您需要檢查您的核心是否啟用 <em>kprobe</em>，如果未啟用，則需要新增以下設定：</p><div class="language- vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>CONFIG_KPROBES=y</span></span>
<span class="line"><span>CONFIG_HAVE_KPROBES=y</span></span>
<span class="line"><span>CONFIG_KPROBE_EVENTS=y</span></span></code></pre></div><p>最後，重新建置您的核心即可。</p><p>如果您發現 KPROBES 仍未生效，很有可能是因為它依賴的 <code>CONFIG_MODULES</code> 並未被啟用，如果還是未生效請輸入 <code>make menuconfig</code> 搜尋 KPROBES 的其他相依性並啟用。</p><p>如果您在整合 KernelSU 之後手機無法啟動，那麼很可能您的核心中 <strong>kprobe 無法正常運作</strong>，您需要修正這個錯誤，或者使用第二種方法。</p><div class="tip custom-block"><p class="custom-block-title">如何檢查 kprobe 是否損毀？</p><p>將 <code>KernelSU/kernel/ksu.c</code> 中的 <code>ksu_sucompat_init()</code> 和 <code>ksu_ksud_init()</code> 註解掉，如果正常開機，即 kprobe 已損毀；或者您可以手動嘗試使用 kprobe 功能，如果不正常，手機會直接重新啟動。</p></div><div class="info custom-block"><p class="custom-block-title">如何為非 GKI 核心啟用卸載模組功能</p><p>如果你的內核版本小於 5.10，你應該將 <code>path_umount</code> 向後移植至 <code>fs/namespace.c</code>。卸載模組功能依賴於這個函數。如果你沒有向後移植 <code>path_umount</code>，卸載模組功能將無法工作。你可以在<a href="#how-to-backport-path_unpount">這裡查看更多關於 <code>path_unmount</code> 的資料</a>。</p></div><h2 id="manually-modify-the-kernel-source" tabindex="-1">手動修改核心原始碼 <a class="header-anchor" href="#manually-modify-the-kernel-source" aria-label="Permalink to &quot;手動修改核心原始碼 {#manually-modify-the-kernel-source}&quot;">​</a></h2><p>如果 kprobe 無法正常運作 (在4.8之前可能是上游或核心的錯誤)，那您可以嘗試這種方法：</p><p>首先，將 KernelSU 新增至您的原始碼樹狀結構，在核心的根目錄執行以下命令：</p><div class="language-sh vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">sh</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">curl</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -LSs</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &quot;https://raw.githubusercontent.com/tiann/KernelSU/main/kernel/setup.sh&quot;</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> |</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> bash</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> -s</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> v0.9.5</span></span></code></pre></div><p>請記住，在某些裝置上，您的 <code>defconfig</code> 可能位於 <code>arch/arm64/configs</code> 中，或在其他情況下位於 <code>arch/arm64/configs/vendor/你的defconfig</code> 中。無論您使用哪個 <code>defconfig</code>，請確保使用 <code>CONFIG_KSU=y</code> 啟用KernelSU，或使用 <code>n</code> 停用它。例如，如果您選擇啟用它，則 <code>defconfig</code> 應包含以下字串：</p><div class="language-conf vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">conf</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span># KernelSU</span></span>
<span class="line"><span>CONFIG_KSU=y</span></span></code></pre></div><p>然後，手動修改核心原始碼，您可以參閱下方的 patch：</p><div class="vp-code-group vp-adaptive-theme"><div class="tabs"><input type="radio" name="group-XMomY" id="tab-R8_7fMD" checked><label data-title="exec.c" for="tab-R8_7fMD">exec.c</label><input type="radio" name="group-XMomY" id="tab-fUkgZ1Q"><label data-title="open.c" for="tab-fUkgZ1Q">open.c</label><input type="radio" name="group-XMomY" id="tab-czXL_eo"><label data-title="read_write.c" for="tab-czXL_eo">read_write.c</label><input type="radio" name="group-XMomY" id="tab-gAt70af"><label data-title="stat.c" for="tab-gAt70af">stat.c</label></div><div class="blocks"><div class="language-diff vp-adaptive-theme active"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/exec.c b/fs/exec.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index ac59664eaecf..bdd585e1d2cc 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/exec.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/exec.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -1890,11 +1890,14 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static int __do_execve_file(int fd, struct filename *filename,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return retval;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern bool ksu_execveat_hook __read_mostly;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_execveat(int *fd, struct filename **filename_ptr, void *argv,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			void *envp, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_execveat_sucompat(int *fd, struct filename **filename_ptr,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+				 void *argv, void *envp, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static int do_execveat_common(int fd, struct filename *filename,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			      struct user_arg_ptr argv,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			      struct user_arg_ptr envp,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			      int flags)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (unlikely(ksu_execveat_hook))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_execveat(&amp;fd, &amp;filename, &amp;argv, &amp;envp, &amp;flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	else</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_execveat_sucompat(&amp;fd, &amp;filename, &amp;argv, &amp;envp, &amp;flags);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return __do_execve_file(fd, filename, argv, envp, flags, NULL);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span></code></pre></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/open.c b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 05036d819197..965b84d486b8 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -348,6 +348,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return ksys_fallocate(fd, mode, offset, len);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			 int *flags);</span></span>
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
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_faccessat(&amp;dfd, &amp;filename, &amp;mode, NULL);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (mode &amp; ~S_IRWXO)	/* where&#39;s F_OK, X_OK, W_OK, R_OK? */</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EINVAL;</span></span></code></pre></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/read_write.c b/fs/read_write.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 650fc7e0f3a6..55be193913b6 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/read_write.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/read_write.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -434,10 +434,14 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> ssize_t kernel_read(struct file *file, void *buf, size_t count, loff_t *pos)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> EXPORT_SYMBOL(kernel_read);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern bool ksu_vfs_read_hook __read_mostly;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_vfs_read(struct file **file_ptr, char __user **buf_ptr,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			size_t *count_ptr, loff_t **pos);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> ssize_t vfs_read(struct file *file, char __user *buf, size_t count, loff_t *pos)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	ssize_t ret;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (unlikely(ksu_vfs_read_hook))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_vfs_read(&amp;file, &amp;buf, &amp;count, &amp;pos);</span></span>
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
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /**</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * vfs_statx - Get basic and extra attributes by filename</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * @dfd: A file descriptor representing the base dir for a relative filename</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -170,6 +172,7 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_statx(int dfd, const char __user *filename, int flags,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int error = -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = LOOKUP_FOLLOW | LOOKUP_AUTOMOUNT;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_stat(&amp;dfd, &amp;filename, &amp;flags);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if ((flags &amp; ~(AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		       AT_EMPTY_PATH | KSTAT_QUERY_FLAGS)) != 0)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EINVAL;</span></span></code></pre></div></div></div><p>主要修改四個項目：</p><ol><li>do_faccessat，通常位於 <code>fs/open.c</code></li><li>do_execveat_common，通常位於 <code>fs/exec.c</code></li><li>vfs_read，通常位於 <code>fs/read_write.c</code></li><li>vfs_statx，通常位於 <code>fs/stat.c</code></li></ol><p>如果您的核心沒有 <code>vfs_statx</code>，使用 <code>vfs_fstatat</code> 將其取代：</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/stat.c b/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 068fdbcc9e26..5348b7bb9db2 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/stat.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -87,6 +87,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_fstat(unsigned int fd, struct kstat *stat)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> EXPORT_SYMBOL(vfs_fstat);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_stat(int *dfd, const char __user **filename_user, int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_fstatat(int dfd, const char __user *filename, struct kstat *stat,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		int flag)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -94,6 +96,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> int vfs_fstatat(int dfd, const char __user *filename, struct kstat *stat,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int error = -EINVAL;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = 0;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_stat(&amp;dfd, &amp;filename, &amp;flag);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if ((flag &amp; ~(AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		      AT_EMPTY_PATH)) != 0)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		goto out;</span></span></code></pre></div><p>對於早於 4.17 的核心，如果沒有 <code>do_faccessat</code>，可以直接找到 <code>faccessat</code> 系統呼叫的定義並進行修改：</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/open.c b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 2ff887661237..e758d7db7663 100644</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/fs/open.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -355,6 +355,9 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return error;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+			        int *flags);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> /*</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * access() needs to use the real uid/gid, not the effective uid/gid.</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * We do this by temporarily clearing all FS-related capabilities and</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -370,6 +373,8 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> SYSCALL_DEFINE3(faccessat, int, dfd, const char __user *, filename, int, mode)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	int res;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	unsigned int lookup_flags = LOOKUP_FOLLOW;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	ksu_handle_faccessat(&amp;dfd, &amp;filename, &amp;mode, NULL);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (mode &amp; ~S_IRWXO)	/* where&#39;s F_OK, X_OK, W_OK, R_OK? */</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		return -EINVAL;</span></span></code></pre></div><h3 id="safe-mode" tabindex="-1">安全模式 <a class="header-anchor" href="#safe-mode" aria-label="Permalink to &quot;安全模式 {#safe-mode}&quot;">​</a></h3><p>若要啟用 KernelSU 內建的安全模式，您還需要修改 <code>drivers/input/input.c</code> 中的 <code>input_handle_event</code> 方法：</p><div class="tip custom-block"><p class="custom-block-title">小建議</p><p>強烈建議啟用此功能，如果遇到開機迴圈，這將會非常有用！</p></div><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/drivers/input/input.c b/drivers/input/input.c</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">index 45306f9ef247..815091ebfca4 100755</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/drivers/input/input.c</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+++ b/drivers/input/input.c</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-light-font-weight:bold;--shiki-dark:#B392F0;--shiki-dark-font-weight:bold;">@@ -367,10 +367,13 @@</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static int input_get_disposition(struct input_dev *dev,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	return disposition;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern bool ksu_input_hook __read_mostly;</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+extern int ksu_handle_input_handle_event(unsigned int *type, unsigned int *code, int *value);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> static void input_handle_event(struct input_dev *dev,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 			       unsigned int type, unsigned int code, int value)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">	int disposition = input_get_disposition(dev, type, code, &amp;value);</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+	if (unlikely(ksu_input_hook))</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+		ksu_handle_input_handle_event(&amp;type, &amp;code, &amp;value);</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> </span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 	if (disposition != INPUT_IGNORE_EVENT &amp;&amp; type != EV_SYN)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 		add_input_randomness(type, code, value);</span></span></code></pre></div><div class="info custom-block"><p class="custom-block-title">不小心進入安全模式？</p><p>如果您使用手動整合且不停用 <code>CONFIG_KPROBES</code>，那麼您將可能會在啟動後透過按下音量來減少按鈕來觸發安全模式！因此，如果使用手動集成，您需要停用 <code>CONFIG_KPROBES</code> ！</p></div><h3 id="failed-to-execute-pm-in-terminal" tabindex="-1">無法在終端中執行 <code>pm</code> ? <a class="header-anchor" href="#failed-to-execute-pm-in-terminal" aria-label="Permalink to &quot;無法在終端中執行 \`pm\` ? {#failed-to-execute-pm-in-terminal}&quot;">​</a></h3><p>你應該修改 <code>fs/devpts/inode.c</code>，參考:</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">diff --git a/fs/devpts/inode.c b/fs/devpts/inode.c</span></span>
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
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">        return dentry-&gt;d_fsdata;</span></span></code></pre></div><h3 id="how-to-backport-path_unpount" tabindex="-1">如何向後移植 path_umount <a class="header-anchor" href="#how-to-backport-path_unpount" aria-label="Permalink to &quot;如何向後移植 path_umount {#how-to-backport-path_unpount}&quot;">​</a></h3><p>你可以透過向後移植 <code>path_umount</code> 來讓卸載模組功能在低於 5.10 的非 GKI 核心上運作。你可以參考這個修改:</p><div class="language-diff vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">--- a/fs/namespace.c</span></span>
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
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  * This is important for filesystems which use unnamed block devices.</span></span></code></pre></div><p>最後，再次建置您的核心，KernelSU 將會如期運作。</p>`,45)])])}const o=i(e,[["render",l]]);export{E as __pageData,o as default};
