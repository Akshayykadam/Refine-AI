import * as FileSystem from 'expo-file-system/legacy';

const REPO_OWNER = 'Akshayykadam';
const REPO_NAME = 'Refine-AI';
const GITHUB_API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`;

export interface ReleaseInfo {
    version: string;
    notes: string;
    downloadUrl: string;
    fileName: string;
}

export const checkUpdate = async (currentVersion: string): Promise<ReleaseInfo | null> => {
    try {
        const response = await fetch(GITHUB_API_URL, {
            headers: {
                'Cache-Control': 'no-cache'
            }
        });
        const data = await response.json();

        if (!data || !data.tag_name) return null;

        const latestVersion = data.tag_name.replace('v', '');

        if (compareVersions(latestVersion, currentVersion) > 0) {
            const apkAsset = data.assets.find((asset: any) => asset.name.endsWith('.apk'));
            if (apkAsset) {
                return {
                    version: latestVersion,
                    notes: data.body,
                    downloadUrl: apkAsset.browser_download_url,
                    fileName: apkAsset.name
                };
            }
        }
    } catch (e) {
        console.error("Update Check Failed", e);
    }
    return null;
};

const compareVersions = (v1: string, v2: string) => {
    const p1 = v1.split('.').map(Number);
    const p2 = v2.split('.').map(Number);
    for (let i = 0; i < Math.max(p1.length, p2.length); i++) {
        const n1 = p1[i] || 0;
        const n2 = p2[i] || 0;
        if (n1 > n2) return 1;
        if (n1 < n2) return -1;
    }
    return 0;
};

export const downloadUpdate = async (url: string, fileName: string, onProgress?: (progress: number) => void): Promise<string> => {
    try {
        if (!url) throw new Error("Download URL is empty");

        // Use cache directory to avoid permission issues
        const fileUri = `${FileSystem.cacheDirectory}${fileName}`;

        // Clean up previous download (using deleteAsync with idempotent to avoid getInfoAsync)
        try {
            await FileSystem.deleteAsync(fileUri, { idempotent: true });
        } catch (e) {
            // Ignore deletion errors
        }

        const downloadResu = FileSystem.createDownloadResumable(
            url,
            fileUri,
            {},
            (downloadProgress) => {
                const progress = downloadProgress.totalBytesWritten / downloadProgress.totalBytesExpectedToWrite;
                if (onProgress) onProgress(progress);
            }
        );

        const result = await downloadResu.downloadAsync();

        if (!result) throw new Error("Download result is null");

        if (result.status !== 200) {
            throw new Error(`Server returned status: ${result.status}`);
        }

        if (!result.uri) throw new Error("Download succeeded but URI is null");

        return result.uri;
    } catch (e: any) {
        console.error("Download Failed", e);
        throw new Error(e.message || "Unknown download error");
    }
};
