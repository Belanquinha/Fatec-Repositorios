import os
import csv
import io
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed

CSV_PATH = os.path.join("back-end", "src", "main", "resources", "seeds", "instituicoes.csv")
OUTPUT_DIR = os.path.join("front-end", "public", "logos-fatec")

def process_institution(codigo, nome, url):
    if not url or not url.startswith("http"):
        return codigo, False, f"URL invalida para {nome}"
    
    target_path = os.path.join(OUTPUT_DIR, f"{codigo}.png")
    if os.path.exists(target_path) and os.path.getsize(target_path) > 1000:
        return codigo, True, f"Ja existe: {target_path}"

    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
        with urllib.request.urlopen(req, timeout=20) as resp:
            data = resp.read()

        with zipfile.ZipFile(io.BytesIO(data)) as z:
            cor_files = [f for f in z.namelist() if not f.startswith("__MACOSX") and f.lower().endswith("_cor.png")]
            if not cor_files:
                # Fallback to any png inside zip
                png_files = [f for f in z.namelist() if not f.startswith("__MACOSX") and f.lower().endswith(".png")]
                if not png_files:
                    return codigo, False, f"Nenhum PNG encontrado no zip de {nome}"
                target_file_in_zip = png_files[0]
            else:
                target_file_in_zip = cor_files[0]

            with open(target_path, "wb") as f_out:
                f_out.write(z.read(target_file_in_zip))

        return codigo, True, f"Sucesso: {nome} -> {codigo}.png"
    except Exception as e:
        return codigo, False, f"Erro ao baixar {nome} ({url}): {e}"

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    instituicoes = []

    with open(CSV_PATH, mode="r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f, delimiter=";")
        for row in reader:
            codigo = row.get("codigoUnidade", "").strip()
            nome = row.get("nome", "").strip()
            url = row.get("linkLogo", "").strip()
            if codigo and url:
                instituicoes.append((codigo, nome, url))

    print(f"Iniciando download e extracao de {len(instituicoes)} logos FATEC...")
    sucessos = 0
    falhas = 0

    with ThreadPoolExecutor(max_workers=8) as executor:
        futures = {executor.submit(process_institution, codigo, nome, url): (codigo, nome) for codigo, nome, url in instituicoes}
        for future in as_completed(futures):
            codigo, ok, msg = future.result()
            if ok:
                sucessos += 1
            else:
                falhas += 1
                print(f"[FALHA] {msg}")

    print(f"\nFinalizado! Total de sucessos: {sucessos} | Falhas: {falhas}")
    print(f"Logos salvas em: {os.path.abspath(OUTPUT_DIR)}")

if __name__ == "__main__":
    main()
