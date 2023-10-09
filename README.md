# Custom Bossbar Textures
This mod allows you to replace vanilla bossbars with highly customizable bossbars using resource packs.
## Usage
1. Create a folder named `cbt` in assets/minecraft/
2. Inside `cbt/` make a folder for your bossbar.
3. Make a file named `*.bossbar`.
4. Add the following text in the created file.
> type=<hotbar type, see below>  
> name=<display name of the hotbar\>  
> texture=<texture name without ".png">  
> overlay=<overlay name without ".png">  
  
Bossbar types: 
  
![types](https://github.com/Vladomeme/custom-bossbar-textures/assets/84517135/c674113a-30d4-4fd8-be65-5e52afc5aafd)

5. Create a texture and an overlay for the hotbar.
   
     Texture and overlay should have the same size. Texture should contain everything and an empty bar. Overlay should only contain the depleting part itself.
   
| ![silver_construct_texture](https://github.com/Vladomeme/custom-bossbar-textures/assets/84517135/09bd9a74-11d6-4e4b-9cf1-67843db45b3d) | ![silver_construct_overlay](https://github.com/Vladomeme/custom-bossbar-textures/assets/84517135/4fa660e3-f83e-40c8-a056-f4ec4f872328) |
|----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| *Texture*                                                                                                                                | *Overlay*                                                                                                                                |

  Size of the bar in overlay will be calculated using leftmost and rightmost non-fully transparent pixels.
  
6. Put the texture and the overlay in the same folder as the `.bossbar` file.

## Phase texture changes
You can change the bossbar texture depending on how much health is left. For that add these lines to your `.bossbar` file.
>texture.<%HP left>=<texture name without ".png">  
>overlay.<%HP left>=<overlay name without ".png">
  
For example:  
>type=normal  
>name=Silver Construct  
>texture=silver_construct_texture_1  
>overlay=silver_construct_overlay_1  
>texture.66.6=silver_construct_texture_2  
>overlay.66.6=silver_construct_overlay_2  
>texture.33.3=silver_construct_texture_3  
>overlay.33.3=silver_construct_overlay_3  
