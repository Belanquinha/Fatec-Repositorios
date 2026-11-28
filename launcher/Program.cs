using System.Diagnostics;
using System.IO.Compression;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text.Json.Serialization;
using Microsoft.Win32;

namespace FatecLauncher;

internal static class Program
{
    [STAThread]
    static void Main(string[] args)
    {
        ApplicationConfiguration.Initialize();

        if (args.Length == 0 || !args[0].StartsWith("fatecrepo://", StringComparison.OrdinalIgnoreCase))
        {
            ProtocolRegistrar.InstalarEExibirFeedback();
            return;
        }

        Application.Run(new LauncherForm(args[0]));
    }
}

internal static class ProtocolRegistrar
{
    private const string Protocolo = "fatecrepo";

    public static void InstalarEExibirFeedback()
    {
        try
        {
            string destinoDir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "FatecLauncher"
            );
            Directory.CreateDirectory(destinoDir);
            string destinoExe = Path.Combine(destinoDir, "FatecLauncher.exe");
            string? atualExe = Environment.ProcessPath;

            if (!string.IsNullOrWhiteSpace(atualExe)
                && !string.Equals(Path.GetFullPath(atualExe), Path.GetFullPath(destinoExe), StringComparison.OrdinalIgnoreCase))
            {
                File.Copy(atualExe, destinoExe, overwrite: true);
            }

            using RegistryKey key = Registry.CurrentUser.CreateSubKey($@"Software\Classes\{Protocolo}");
            key.SetValue("", "URL:Fatec Launcher Protocol");
            key.SetValue("URL Protocol", "");

            using RegistryKey cmdKey = key.CreateSubKey(@"shell\open\command");
            cmdKey.SetValue("", $"\"{destinoExe}\" \"%1\"");

            MessageBox.Show(
                "Fatec Launcher instalado com sucesso!\n\nAgora você pode voltar ao navegador e clicar em 'Jogar' em qualquer jogo do Fatec-Repositorios.",
                "Fatec Launcher",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information
            );
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                $"Não foi possível registrar o Fatec Launcher:\n{ex.Message}",
                "Erro — Fatec Launcher",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error
            );
        }
    }
}

internal sealed class LauncherForm : Form
{
    private readonly string _uriRaw;
    private readonly Label _lblTitulo;
    private readonly Label _lblStatus;
    private readonly ProgressBar _progressBar;

    public LauncherForm(string uriRaw)
    {
        _uriRaw = uriRaw;

        Text = "Fatec Launcher";
        Width = 460;
        Height = 200;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        BackColor = Color.FromArgb(18, 18, 20);
        ForeColor = Color.FromArgb(244, 246, 249);

        _lblTitulo = new Label
        {
            Text = "Preparando jogo...",
            Font = new Font("Segoe UI", 12, FontStyle.Bold),
            ForeColor = Color.FromArgb(220, 167, 3),
            Location = new Point(24, 22),
            Size = new Size(400, 28)
        };

        _lblStatus = new Label
        {
            Text = "Conectando ao servidor...",
            Font = new Font("Segoe UI", 9.5f),
            Location = new Point(24, 58),
            Size = new Size(400, 24)
        };

        _progressBar = new ProgressBar
        {
            Location = new Point(24, 96),
            Size = new Size(396, 24),
            Style = ProgressBarStyle.Marquee
        };

        Controls.Add(_lblTitulo);
        Controls.Add(_lblStatus);
        Controls.Add(_progressBar);

        Shown += async (_, _) => await ExecutarFluxoAsync();
    }

    private async Task ExecutarFluxoAsync()
    {
        string? pastaSessao = null;
        try
        {
            var (apiBase, ticket) = ParseUri(_uriRaw);
            LimparSessoesAntigas();

            using var http = new HttpClient { Timeout = TimeSpan.FromMinutes(15) };
            var meta = await http.GetFromJsonAsync<SessaoMetadata>(
                $"{apiBase.TrimEnd('/')}/jogos/sessoes/{Uri.EscapeDataString(ticket)}"
            ) ?? throw new InvalidOperationException("Metadados da sessão não encontrados.");

            _lblTitulo.Text = meta.Titulo;
            pastaSessao = Path.Combine(Path.GetTempPath(), "FatecLauncher", $"run_{Guid.NewGuid():N}");
            string pastaJogo = Path.Combine(pastaSessao, "jogo");
            Directory.CreateDirectory(pastaJogo);
            string zipPath = Path.Combine(pastaSessao, "pacote.zip");

            await BaixarComProgressoAsync(
                http,
                $"{apiBase.TrimEnd('/')}/jogos/sessoes/{Uri.EscapeDataString(ticket)}/arquivo",
                zipPath,
                meta.TamanhoBytes
            );

            if (!string.IsNullOrWhiteSpace(meta.Sha256))
            {
                _lblStatus.Text = "Verificando integridade do pacote...";
                string hash = await CalcularSha256Async(zipPath);
                if (!string.Equals(hash, meta.Sha256, StringComparison.OrdinalIgnoreCase))
                {
                    throw new InvalidOperationException("Falha na verificação de integridade (SHA-256).");
                }
            }

            _lblStatus.Text = "Extraindo arquivos na pasta temporária...";
            _progressBar.Style = ProgressBarStyle.Marquee;
            await Task.Run(() => ExtrairSeguro(zipPath, pastaJogo));
            try { File.Delete(zipPath); } catch { }

            string exePath = Path.GetFullPath(Path.Combine(pastaJogo, meta.ExecutavelPrincipal.Replace('/', Path.DirectorySeparatorChar)));
            string pastaJogoFull = Path.GetFullPath(pastaJogo);
            if (!exePath.StartsWith(pastaJogoFull, StringComparison.OrdinalIgnoreCase) || !File.Exists(exePath))
            {
                throw new FileNotFoundException("Executável principal não encontrado após extração.");
            }

            _lblStatus.Text = "Jogo em execução...";
            WindowState = FormWindowState.Minimized;

            using var proc = Process.Start(new ProcessStartInfo
            {
                FileName = exePath,
                WorkingDirectory = Path.GetDirectoryName(exePath)!,
                UseShellExecute = true
            }) ?? throw new InvalidOperationException("Não foi possível iniciar o executável do jogo.");

            await proc.WaitForExitAsync();
        }
        catch (Exception ex)
        {
            WindowState = FormWindowState.Normal;
            MessageBox.Show(
                $"Não foi possível iniciar o jogo:\n{ex.Message}",
                "Erro — Fatec Launcher",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error
            );
        }
        finally
        {
            if (pastaSessao != null)
            {
                try { Directory.Delete(pastaSessao, recursive: true); } catch { }
            }
            Close();
        }
    }

    private async Task BaixarComProgressoAsync(HttpClient http, string url, string destino, long totalEsperado)
    {
        using var resp = await http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
        resp.EnsureSuccessStatusCode();
        long total = resp.Content.Headers.ContentLength ?? totalEsperado;

        _progressBar.Style = total > 0 ? ProgressBarStyle.Continuous : ProgressBarStyle.Marquee;
        _progressBar.Minimum = 0;
        _progressBar.Maximum = 100;

        await using var input = await resp.Content.ReadAsStreamAsync();
        await using var output = File.Create(destino);
        var buffer = new byte[81920];
        long baixado = 0;
        int lido;
        while ((lido = await input.ReadAsync(buffer)) > 0)
        {
            await output.WriteAsync(buffer.AsMemory(0, lido));
            baixado += lido;
            if (total > 0)
            {
                int pct = (int)Math.Clamp(baixado * 100 / total, 0, 100);
                _progressBar.Value = pct;
                _lblStatus.Text = $"Baixando jogo... {pct}% ({baixado / 1024 / 1024} MB / {total / 1024 / 1024} MB)";
            }
        }
    }

    private static (string apiBase, string ticket) ParseUri(string raw)
    {
        var uri = new Uri(raw);
        var query = uri.Query.TrimStart('?')
            .Split('&', StringSplitOptions.RemoveEmptyEntries)
            .Select(p => p.Split('=', 2))
            .ToDictionary(
                p => Uri.UnescapeDataString(p[0]),
                p => p.Length > 1 ? Uri.UnescapeDataString(p[1]) : ""
            );

        if (!query.TryGetValue("api", out var api) || !query.TryGetValue("ticket", out var ticket)
            || string.IsNullOrWhiteSpace(api) || string.IsNullOrWhiteSpace(ticket))
        {
            throw new ArgumentException("Link do Fatec Launcher inválido.");
        }

        var apiUri = new Uri(api);
        bool localDev = apiUri.Host.Equals("localhost", StringComparison.OrdinalIgnoreCase)
            || apiUri.Host.Equals("127.0.0.1", StringComparison.OrdinalIgnoreCase);
        if (!localDev && !apiUri.Scheme.Equals("https", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Origem da API não permitida.");
        }

        return (api, ticket);
    }

    private static void ExtrairSeguro(string zipPath, string destinoDir)
    {
        string destinoFull = Path.GetFullPath(destinoDir);
        long totalExtraido = 0;
        const long maxExtraido = 2L * 1024 * 1024 * 1024;

        using ZipArchive archive = ZipFile.OpenRead(zipPath);
        foreach (ZipArchiveEntry entry in archive.Entries)
        {
            string caminhoDestino = Path.GetFullPath(Path.Combine(destinoFull, entry.FullName));
            if (!caminhoDestino.StartsWith(destinoFull, StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidDataException("Entrada insegura detectada no arquivo .zip (Zip Slip).");
            }

            if (string.IsNullOrEmpty(entry.Name))
            {
                Directory.CreateDirectory(caminhoDestino);
                continue;
            }

            Directory.CreateDirectory(Path.GetDirectoryName(caminhoDestino)!);
            totalExtraido += entry.Length;
            if (totalExtraido > maxExtraido)
            {
                throw new InvalidDataException("Pacote excede o limite de segurança de 2GB ao extrair.");
            }
            entry.ExtractToFile(caminhoDestino, overwrite: true);
        }
    }

    private static async Task<string> CalcularSha256Async(string path)
    {
        await using var stream = File.OpenRead(path);
        byte[] hash = await SHA256.HashDataAsync(stream);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private static void LimparSessoesAntigas()
    {
        try
        {
            string raiz = Path.Combine(Path.GetTempPath(), "FatecLauncher");
            if (!Directory.Exists(raiz)) return;
            foreach (string dir in Directory.GetDirectories(raiz, "run_*"))
            {
                try { Directory.Delete(dir, recursive: true); } catch { }
            }
        }
        catch { }
    }

    private sealed record SessaoMetadata(
        [property: JsonPropertyName("projetoId")] string ProjetoId,
        [property: JsonPropertyName("titulo")] string Titulo,
        [property: JsonPropertyName("nomeOriginal")] string NomeOriginal,
        [property: JsonPropertyName("tamanhoBytes")] long TamanhoBytes,
        [property: JsonPropertyName("sha256")] string? Sha256,
        [property: JsonPropertyName("executavelPrincipal")] string ExecutavelPrincipal
    );
}
