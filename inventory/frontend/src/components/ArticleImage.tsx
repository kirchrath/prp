export function ArticleImage({ url, size = 48 }: { url?: string; size?: number }) {
  return (
    <figure className="image article-image" style={{ width: size, height: size }}>
      {url ? <img src={url} alt="" loading="lazy" referrerPolicy="no-referrer" /> : <span className="placeholder">📦</span>}
    </figure>
  );
}
