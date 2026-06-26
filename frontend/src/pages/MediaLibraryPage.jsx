import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Image, Loader2, Plus } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";

export default function MediaLibraryPage() {
  const queryClient = useQueryClient();
  const [folderName, setFolderName] = useState("");
  const [image, setImage] = useState({ folderId: "", url: "", altText: "" });
  const folders = useQuery({ queryKey: ["media-folders"], queryFn: marketplaceApi.mediaFolders });
  const images = useQuery({ queryKey: ["media-images"], queryFn: marketplaceApi.mediaImages });
  const createFolder = useMutation({
    mutationFn: marketplaceApi.createMediaFolder,
    onSuccess: () => {
      setFolderName("");
      queryClient.invalidateQueries({ queryKey: ["media-folders"] });
    }
  });
  const addImage = useMutation({
    mutationFn: marketplaceApi.addMediaImage,
    onSuccess: () => {
      setImage({ folderId: "", url: "", altText: "" });
      queryClient.invalidateQueries({ queryKey: ["media-images"] });
      queryClient.invalidateQueries({ queryKey: ["media-folders"] });
    }
  });

  if (folders.isLoading || images.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải media...</section>;
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Image size={16} /> Media Library</span>
          <h1>Thu vien anh</h1>
          <p className="muted">Quản lý thư mục và ảnh URL cho sản phẩm/chat/đơn custom.</p>
        </div>
      </section>

      <section className="content-grid">
        <form className="panel inline-form" onSubmit={(event) => {
          event.preventDefault();
          if (folderName.trim()) createFolder.mutate({ name: folderName });
        }}>
          <input placeholder="Ten folder" value={folderName} onChange={(event) => setFolderName(event.target.value)} />
          <button className="btn primary" type="submit"><Plus size={16} /> Folder</button>
        </form>
        <form className="panel catalog-form compact-admin-form" onSubmit={(event) => {
          event.preventDefault();
          addImage.mutate({ ...image, folderId: Number(image.folderId || folders.data?.[0]?.id) });
        }}>
          <select value={image.folderId} onChange={(event) => setImage({ ...image, folderId: event.target.value })}>
            {(folders.data || []).map((folder) => <option key={folder.id} value={folder.id}>{folder.name}</option>)}
          </select>
          <input placeholder="Image URL" value={image.url} onChange={(event) => setImage({ ...image, url: event.target.value })} />
          <input placeholder="Alt text" value={image.altText} onChange={(event) => setImage({ ...image, altText: event.target.value })} />
          <button className="btn primary" type="submit">Thêm ảnh</button>
        </form>
      </section>

      <section className="panel">
        <h2>Folders</h2>
        <div className="module-strip">
          {(folders.data || []).map((folder) => <span key={folder.id}>{folder.name} ({folder.imageCount})</span>)}
        </div>
      </section>

      <section className="product-grid mini-grid">
        {(images.data || []).map((item) => (
          <article className="mini-product" key={item.id}>
            <img src={item.url} alt={item.altText} />
            <strong>{item.altText || "Anh media"}</strong>
            <span>Folder #{item.folderId}</span>
          </article>
        ))}
      </section>
    </div>
  );
}
